#!/usr/bin/env boot

(set-env!
  :source-paths #{"src" "test"}
  :resource-paths #{"resources"}
  :dependencies '[[org.clojure/clojure "1.8.0"]
                  [adzerk/boot-test "1.1.2" :scope "test"]
                  [com.itextpdf/kernel "7.0.1"]
                  [com.itextpdf/io "7.0.1"]
                  [com.itextpdf/layout "7.0.1"]
                  [org.slf4j/slf4j-simple "1.7.21"] ;; Logging
                  ;; For BlockCipher decryption
                  [org.bouncycastle/bcprov-jdk15on "1.49"]
                  [org.bouncycastle/bcpkix-jdk15on "1.49"]
                  [org.clojure/data.json "0.2.6"]
                  [org.clojure/data.codec "0.1.0"]
                  [ring "1.5.0"]
                  [ring/ring-codec "1.0.1"]
                  [ring/ring-jetty-adapter "1.3.1"]])

(require '[adzerk.boot-test :refer :all])

(task-options!
  pom {:project 'watermarker
       :version "0.1.0"}
  aot {:namespace '#{watermarker.core}}
  jar {:main 'watermarker.core
       :manifest {"Description" "Watermarking Service"}})

(deftask build
  "Build uberjar"
  []
  (comp (aot) (pom) (uber)
        (jar :file "watermarker.jar")
        (sift :include #{#"watermarker.jar"})
        (target "target")))
