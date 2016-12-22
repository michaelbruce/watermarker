(ns watermarker.data
  (:require [clojure.walk :as walk]
            [clojure.data.json :only [read-str] :as json]
            [clojure.java.io :as io :only [file resource]]
            [clojure.string :as str :only [split]])
  (:import [java.util Base64]
           [com.itextpdf.kernel.crypto]
           [javax.crypto Cipher]
           [javax.crypto.spec SecretKeySpec IvParameterSpec]
           [java.net URLDecoder]))

(def private-key (.getBytes "strong-private-key"))
(def iv (.getBytes "example-iv"))

(def default-map
  {:ownerPassword "example-password"
   :userPassword ""
   :personalisationMessage "This report is prepared solely for the use of {!Contact.Name}"
   :personalisationColor "BLACK"
   :personalisationAlignment "LEFT"
   :personalisationAngle "0"
   :personalisationFont "Helvetica"
   :personalisationFontSize "10"
   :personalisationXOffset "30"
   :personalisationYOffset "30"
   :relativeOffset  "false"
   :relativeYOffset  "top"
   :stampDensity "100"
   :stampPageThreshold "1"
   :stampPagePeriod "1"
   :pageSize "A4"
   :disableCopy "true"
   :disableAssembly "true"
   :disableFillIn "true"
   :disableModifyAnnotation "true"
   :disableModifyContent "true"
   :disablePrinting "true"
   :disableScreenreaders "true"})

(defn load-properties-file
  "loads a .properties file from the resources folder"
  [file-name]
  (with-open [^java.io.Reader reader (clojure.java.io/reader (io/resource (str "templates/" file-name)))] 
    (merge default-map (let [props (java.util.Properties.)]
                         (.load props reader)
                         (into {} (for [[k v] props]
                                    [(keyword k)
                                     (if (re-matches #"[0-9]+" v) (Integer. v) v)]))))))

(defn- data-as-map
   [request]
   ((walk/keywordize-keys
      (apply hash-map
             (str/split request #"(&|=)"))) :data))

(defn decrypt-string [message]
  (def original (.decode (. Base64 getDecoder) message))
  (let [cipher (Cipher/getInstance "AES/CBC/PKCS5Padding")]
    (.init cipher Cipher/DECRYPT_MODE,
           (SecretKeySpec. private-key, "AES"),
           (IvParameterSpec. iv))
    (def decrypted (.doFinal cipher original))) (String. decrypted, "UTF-8"))

(defn decrypt-http-request
  "Takes params for an http request as decrypts the data parameter"
  [http-request]
  (-> (URLDecoder/decode (data-as-map (http-request :query-string)) "UTF-8")
      decrypt-string
      json/read-str))

