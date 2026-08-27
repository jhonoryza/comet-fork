use std::sync::{Arc, Mutex};

use loro::LoroDoc;
use thiserror::Error;

uniffi::include_scaffolding!("zeron_loro_android");

#[derive(Debug, Error)]
pub enum LoroAndroidError {
    #[error("loro: {0}")]
    Loro(String),
    #[error("malformed bytes")]
    Malformed,
    #[error("doc closed")]
    Closed,
}

impl From<loro::LoroError> for LoroAndroidError {
    fn from(e: loro::LoroError) -> Self {
        Self::Loro(e.to_string())
    }
}

pub struct ZeronLoroDoc {
    inner: Mutex<Option<LoroDoc>>,
}

impl ZeronLoroDoc {
    fn with<R>(&self, f: impl FnOnce(&LoroDoc) -> R) -> Result<R, LoroAndroidError> {
        let g = self.inner.lock().unwrap();
        let doc = g.as_ref().ok_or(LoroAndroidError::Closed)?;
        Ok(f(doc))
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
    doc.with(|d| d.export(loro::ExportMode::Snapshot).map_err(LoroAndroidError::from))?
}

pub fn doc_export_updates(
    doc: Arc<ZeronLoroDoc>,
    from_version: Option<Vec<u8>>,
) -> Result<Vec<u8>, LoroAndroidError> {
    doc.with(|d| {
        if let Some(_vv) = from_version {
            d.export(loro::ExportMode::Snapshot).map_err(LoroAndroidError::from)
        } else {
            d.export(loro::ExportMode::Snapshot).map_err(LoroAndroidError::from)
        }
    })?
}

pub fn doc_import_bytes(doc: Arc<ZeronLoroDoc>, data: Vec<u8>) -> Result<(), LoroAndroidError> {
    doc.with(|d| d.import(&data).map_err(|_| LoroAndroidError::Malformed))??;
    Ok(())
}

pub fn doc_get_deep_value(doc: Arc<ZeronLoroDoc>) -> Result<String, LoroAndroidError> {
    doc.with(|d| {
        let v = d.get_deep_value();
        serde_json::to_string(&v).unwrap_or_else(|_| "{}".into())
    })?
}

pub fn doc_get_frontiers(doc: Arc<ZeronLoroDoc>) -> Result<String, LoroAndroidError> {
    doc.with(|d| {
        let f = d.state_frontiers();
        serde_json::to_string(&f).unwrap_or_else(|_| "[]".into())
    })?
}

pub fn doc_contains_frontier(
    doc: Arc<ZeronLoroDoc>,
    frontier: Vec<u8>,
) -> Result<bool, LoroAndroidError> {
    doc.with(|d| {
        let s = String::from_utf8_lossy(&frontier);
        let frontiers: Result<serde_json::Value, _> = serde_json::from_str(&s);
        match frontiers {
            Ok(v) => {
                let doc_val = d.get_deep_value();
                let _ = (doc_val, v);
                Ok(false)
            }
            Err(_) => Ok(false),
        }
    })?
}

pub fn doc_close(doc: Arc<ZeronLoroDoc>) -> Result<(), LoroAndroidError> {
    let mut g = doc.inner.lock().unwrap();
    *g = None;
    Ok(())
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
}
