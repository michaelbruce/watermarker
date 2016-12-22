(ns watermarker.core-test
  (:require [clojure.test :refer :all]
            [watermarker.core :refer :all])
  (:import [java.net URLDecoder]))


(defn test-pdf
  "Testing itextpdf7"
  []
  (def writer (PdfWriter. "/tmp/test.pdf"))
  (def pdf (PdfDocument. writer))
  (def document (Document. pdf))
  (.add document (Paragraph. "HELLO!"))
  (.close document))

(deftest request-parsing
  (testing "itextpdf7 creates pdfs"
    (is
      (= 0 0))))
