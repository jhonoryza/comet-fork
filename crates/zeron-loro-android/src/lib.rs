use std::sync::{Arc, Mutex};

use loro::LoroDoc;
use thiserror::Error;

#[derive(Debug, Error)]
pub enum LoroAndroidError {
    #[error("loro: {0}")]
    Loro(String),
    #[error("malformed bytes")]
    Malformed,
    #[error("doc closed")]
    Closed,
}

pub struct ZeronLoroDoc {
    inner: Mutex<Option<LoroDoc>>,
}

impl ZeronLoroDoc {
    fn with<R>(&self, f: impl FnOnce(&LoroDoc) -> Result<R, LoroAndroidError>) -> Result<R, LoroAndroidError> {
        let g = self.inner.lock().unwrap();
        let doc = g.as_ref().ok_or(LoroAndroidError::Closed)?;
        f(doc)
    }
}

pub fn create_doc() -> Arc<ZeronLoroDoc> {
    Arc::new(ZeronLoroDoc {
        inner: Mutex::new(Some(LoroDoc::new())),
    })
}

pub fn doc_from_bytes(data: Vec<u8>) -> Result<Arc<ZeronLoroDoc>, LoroAndroidError> {
    let doc = LoroDoc::new();
    doc.import(&data).map_err(|_| LoroAndroidError::Malformed)?;
    Ok(Arc::new(ZeronLoroDoc {
        inner: Mutex::new(Some(doc)),
    }))
}

pub fn doc_export_snapshot(doc: Arc<ZeronLoroDoc>) -> Result<Vec<u8>, LoroAndroidError> {
    doc.with(|d| d.export(loro::ExportMode::Snapshot).map_err(|e| LoroAndroidError::Loro(e.to_string())))
}

pub fn doc_export_updates(
    doc: Arc<ZeronLoroDoc>,
    _from_version: Option<Vec<u8>>,
) -> Result<Vec<u8>, LoroAndroidError> {
    doc.with(|d| d.export(loro::ExportMode::Snapshot).map_err(|e| LoroAndroidError::Loro(e.to_string())))
}

pub fn doc_import_bytes(doc: Arc<ZeronLoroDoc>, data: Vec<u8>) -> Result<(), LoroAndroidError> {
    doc.with(|d| {
        d.import(&data).map_err(|_| LoroAndroidError::Malformed)?;
        Ok(())
    })
}

pub fn doc_get_deep_value(doc: Arc<ZeronLoroDoc>) -> Result<String, LoroAndroidError> {
    doc.with(|d| {
        let v = d.get_deep_value();
        Ok(serde_json::to_string(&v).unwrap_or_else(|_| "{}".into()))
    })
}

pub fn doc_get_frontiers(doc: Arc<ZeronLoroDoc>) -> Result<String, LoroAndroidError> {
    doc.with(|d| {
        let f = d.state_frontiers();
        Ok(format!("{f:?}"))
    })
}

pub fn doc_contains_frontier(
    doc: Arc<ZeronLoroDoc>,
    frontier: Vec<u8>,
) -> Result<bool, LoroAndroidError> {
    doc.with(|_d| {
        let s = String::from_utf8_lossy(&frontier);
        let _v: Result<serde_json::Value, _> = serde_json::from_str(&s);
        Ok(false)
    })
}

pub fn doc_close(doc: Arc<ZeronLoroDoc>) -> Result<(), LoroAndroidError> {
    let mut g = doc.inner.lock().unwrap();
    *g = None;
    Ok(())
}

// ── C ABI (dyn-loaded from Kotlin via JNI `registerNativeMethods`, or raw)
//    These are the single stable entry points the Android `System.loadLibrary`
//    boundary needs. Handles are opaque `*mut ZeronLoroDoc` passed by Kotlin.

#[unsafe(no_mangle)]
pub extern "C" fn zla_create() -> *mut std::ffi::c_void {
    Arc::into_raw(create_doc()) as *mut std::ffi::c_void
}

#[unsafe(no_mangle)]
pub extern "C" fn zla_read(handle: *mut std::ffi::c_void) -> *mut std::ffi::c_char {
    if handle.is_null() { return std::ptr::null_mut() }
    // Borrow without taking the Arc: the pointer is owned by Kotlin until
    // zla_free. Reconstruct a transient Arc from a clone reference is unsafe;
    // instead increment the refcount properly.
    let arc: Arc<ZeronLoroDoc> = {
        let ptr = handle as *const ZeronLoroDoc;
        unsafe { Arc::increment_strong_count(ptr); Arc::from_raw(ptr) }
    };
    let json = doc_get_deep_value(arc).unwrap_or_else(|_| "{}".into());
    std::ffi::CString::new(json).unwrap_or_default().into_raw()
}

#[unsafe(no_mangle)]
pub extern "C" fn zla_import(handle: *mut std::ffi::c_void, data: *const u8, len: usize) -> i32 {
    if handle.is_null() { return 2 }
    let arc: Arc<ZeronLoroDoc> = {
        let ptr = handle as *const ZeronLoroDoc;
        unsafe { Arc::increment_strong_count(ptr); Arc::from_raw(ptr) }
    };
    let slice = unsafe { std::slice::from_raw_parts(data, len) };
    let ok = {
        let g = arc.inner.lock().unwrap();
        match g.as_ref() {
            Some(doc) => doc.import(slice).is_ok(),
            None => false,
        }
    };
    drop(arc);
    if ok { 0 } else { 1 }
}

#[unsafe(no_mangle)]
pub extern "C" fn zla_free(handle: *mut std::ffi::c_void) {
    if handle.is_null() { return }
    let ptr = handle as *const ZeronLoroDoc;
    unsafe { Arc::from_raw(ptr) }; // drops the owned ref from the Kotlin side
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn empty_roundtrip() {
        let doc = create_doc();
        let bytes = doc_export_snapshot(doc.clone()).unwrap();
        let doc2 = doc_from_bytes(bytes.clone()).unwrap();
        let bytes2 = doc_export_snapshot(doc2).unwrap();
        assert!(!bytes.is_empty());
        assert!(!bytes2.is_empty());
    }

    #[test]
    fn malformed_rejected() {
        assert!(doc_from_bytes(vec![0, 1, 2, 3]).is_err());
        let doc = create_doc();
        assert!(doc_import_bytes(doc, vec![0xFF, 0xFF]).is_err());
    }

    #[test]
    fn cabi_roundtrip() {
        let h = zla_create();
        assert!(!h.is_null());
        // zla_read on empty doc returns valid JSON
        let s = zla_read(h);
        assert!(!s.is_null());
        let json = unsafe { std::ffi::CStr::from_ptr(s).to_string_lossy().into_owned() };
        assert_eq!("{}", json);
        unsafe { std::ffi::CString::from_raw(s) };
        zla_free(h);
    }
}
