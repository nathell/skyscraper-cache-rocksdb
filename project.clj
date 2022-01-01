(defproject skyscraper-cache-rocksdb "0.1.0-SNAPSHOT"
  :description "A Skyscraper cache backed by RocksDB."
  :license {:name "MIT", :url "https://github.com/nathell/skyscraper/blob/master/README.md#license"}
  :scm {:name "git", :url "https://github.com/nathell/skyscraper"}
  :codox {:metadata {:doc/format :markdown}}
  :url "https://github.com/nathell/skyscraper-cache-rocksdb"
  :dependencies [[com.taoensso/nippy "3.1.1"]
                 [org.clojure/clojure "1.10.3"]
                 [org.rocksdb/rocksdbjni "6.27.3"]
                 [skyscraper "0.3.0"]])
