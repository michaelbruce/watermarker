(ns watermarker.core-test
  (:require [clojure.test :refer :all]
            [ring.util.codec :refer [url-encode url-decode]]
            [watermarker.core :refer :all]
            [watermarker.data :refer :all]))

(deftest integration-test
  (testing "that a watermarked pdf is returned"))

;; (decrypt-http-request (url-encode {:uri "test-uri"
;;                        :query-string {"template" "generic.properties"
;;                                       "url" "http://www.orimi.com/pdf-test.pdf"}}))

;; XXX move to data-test
(deftest request-parsing
  (testing "my sanity - making sure default map works"
    (is (= (:align default-map) "center")))

  (testing "properties file loads values as correct types"
    (let [values (load-properties-file "generic.properties")]
      (is (= true
             (= (:font-size values) 12)
             (= (:angle values) 0)
             (= (:allow-copy values) true)))))

  (testing "making sure data in GET request is properly decrypted"
    (is (= (decrypt-string (encrypt-string "you don't know"))
           "you don't know"))))
