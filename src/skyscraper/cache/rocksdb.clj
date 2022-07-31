(ns skyscraper.cache.rocksdb
  (:require [skyscraper.cache :as cache]
            [taoensso.nippy :as nippy])
  (:import [skyscraper.cache CacheBackend]
           [java.io Closeable]
           [org.rocksdb CompressionType Options RocksDB]))

(defn- db-keys [key]
  {:meta-key (str "meta/" key)
   :blob-key (str "blobs/" key)})

(deftype RocksDBCache
    [db]
  CacheBackend
  (save-blob [cache key blob metadata]
    (let [{:keys [meta-key blob-key]} (db-keys key)]
      (.put db (nippy/freeze meta-key) (nippy/freeze metadata))
      (.put db (nippy/freeze blob-key) blob)))
  (load-blob [cache key]
    (let [{:keys [meta-key blob-key]} (db-keys key)
          meta (.get db (nippy/freeze meta-key))
          blob (.get db (nippy/freeze blob-key))]
      (when (and meta blob)
        {:meta (nippy/thaw meta)
         :blob blob})))
  Closeable
  (close [cache]
    (.cancelAllBackgroundWork db true)
    (.syncWal db)
    (.close db)
    nil))

(defn rocks-db-cache [dir]
  (let [opts (doto (Options.)
               (.setCreateIfMissing true)
               (.setCompressionType CompressionType/LZ4_COMPRESSION)
               (.setBottommostCompressionType CompressionType/ZSTD_COMPRESSION))
        db (RocksDB/open opts dir)]
    (RocksDBCache. db)))
