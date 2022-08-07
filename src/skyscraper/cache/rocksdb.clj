(ns skyscraper.cache.rocksdb
  (:require [clojure.string :as string]
            [skyscraper.cache :as cache]
            [taoensso.nippy :as nippy])
  (:import [clojure.lang Seqable]
           [java.io
            ByteArrayInputStream ByteArrayOutputStream Closeable
            DataInputStream DataOutputStream]
           [org.rocksdb CompressionType Options RocksDB RocksIterator]
           [skyscraper.cache CacheBackend]))

(defn- read-value [^bytes content]
  (let [bais (ByteArrayInputStream. content)
        dis (DataInputStream. bais)
        metadata (nippy/thaw-from-in! dis)
        blob (nippy/thaw-from-in! dis)]
    {:meta metadata, :blob blob}))

(defn- kv-seq [^RocksIterator iter]
  (lazy-seq
   (when (.isValid iter)
     (let [k (.key iter)
           v (.value iter)]
       (.next iter)
       (cons (assoc (read-value v) :key (String. k "UTF-8"))
             (kv-seq iter))))))

(deftype RocksDBCache
    [db]
  CacheBackend
  (save-blob [cache key blob metadata]
    (let [keyb (.getBytes key "UTF-8")
          baos (ByteArrayOutputStream.)
          dos (DataOutputStream. baos)
          metadata (into {} metadata)]
      (nippy/freeze-to-out! dos metadata)
      (nippy/freeze-to-out! dos blob)
      (.put db keyb (.toByteArray baos))))
  (load-blob [cache key]
    (when-let [content (.get db (.getBytes key "UTF-8"))]
      (read-value content)))
  Seqable
  (seq [cache]
    (let [iter (.newIterator db)]
      (.seekToFirst iter)
      (kv-seq iter)))
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
