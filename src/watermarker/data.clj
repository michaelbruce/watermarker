(ns watermarker.data
  (:require [clojure.walk :as walk]
            [clojure.data.json :only [read-str] :as json]
            [clojure.java.io :as io :only [file resource]]
            [clojure.string :refer [split includes?]]
            [clojure.data.codec.base64 :refer [encode decode]]
            [ring.util.codec :refer [url-encode url-decode]])
  (:import [java.util Base64]
           [com.itextpdf.kernel.crypto]
           [javax.crypto Cipher]
           [javax.crypto.spec SecretKeySpec IvParameterSpec]
           [java.net URLDecoder]))


(def private-key (byte-array (map byte '(6 3 7 8 3 7 6 2 3 4 7 8 9 8 7 7))))
(def iv (byte-array (map byte '(8 3 7 3 6 4 8 2 9 2 8 2 7 6 4 3))))

(def default-map
  {:owner-password "example-password"
   :user-password "strong-password"
   :message "Owned by {!Name}"
   :color "black"
   :align "center"
   :angle 0
   :font "Helvetica"
   :font-size 12
   :x-position 30
   :y-position 30
   :opacity 100
   :page-size "A4"
   :allow-copy true
   :allow-printing true})

(defn value-as-type [value]
  (cond (includes? #"[0-9]+" value) (Integer. value)
        (includes? #"^(?i)(true|false)$" value) (Boolean. value)
        :else value))

(defn load-properties-file
  "loads a .properties file from the resources folder"
  [file-name]
  (with-open [^java.io.Reader reader
              (clojure.java.io/reader (io/resource (str "templates/" file-name)))]
    (merge default-map (let [props (java.util.Properties.)]
                         (.load props reader)
                         (into {} (for [[k v] props]
                                    [(keyword k) (value-as-type v)]))))))

(defn- data-as-map
  [request]
  ((walk/keywordize-keys
     (apply hash-map
            (split request #"(&|=)"))) :data))

(defn decrypt-string [message]
  (let [cipher (Cipher/getInstance "AES/CBC/PKCS5Padding")]
    (.init cipher
           Cipher/DECRYPT_MODE,
           (SecretKeySpec. private-key, "AES"),
           (IvParameterSpec. iv))
    (String. (.doFinal cipher (decode message)) "UTF-8")))

(defn encrypt-string [message]
  (let [cipher (Cipher/getInstance "AES/CBC/PKCS5Padding")]
    (.init cipher
           Cipher/ENCRYPT_MODE
           (SecretKeySpec. private-key "AES")
           (IvParameterSpec. iv))
    (encode (.doFinal cipher (.getBytes message)))))

(defn decrypt-http-request
  "Takes params for an http request as decrypts the data parameter"
  [http-request]
  (-> (URLDecoder/decode (data-as-map (:query-string http-request)) "UTF-8")
      decrypt-string
      json/read-str))

;; (json/read-str
;;   (url-decode
;;     (decrypt-string
;;       (encrypt-string
;;         (url-encode {"template" "generic.properties"
;;                      "url" "http://www.orimi.com/pdf-test.pdf"})))))

