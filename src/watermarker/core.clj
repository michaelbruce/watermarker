(ns watermarker.core
  (:require [watermarker.data :as data]
            [clojure.data.json :only [read-str] :as json]
            [clojure.java.io :as io :only [file resource]]
            [ring.adapter.jetty :as jetty]
            [ring.util.io :as rio]
            [ring.util.response :refer [redirect]])
  (:import [com.itextpdf.kernel.pdf PdfWriter PdfDocument PdfReader]
           [com.itextpdf.kernel.font PdfFontFactory]
           [com.itextpdf.io.font FontProgramFactory FontConstants]
           [com.itextpdf.layout Document]
           [com.itextpdf.layout.element Paragraph]
           [com.itextpdf.layout.property TextAlignment VerticalAlignment]
           [java.io PipedOutputStream PipedInputStream])
  (:gen-class))

(defn serve-content [filetype content]
  {:status  200
   :headers {"Content-Type" filetype}
   :body    content})

(defn stream-file
  "Takes an s3 url an creates an input stream for it's content"
  [s3Url]
  (io/input-stream (io/as-url s3Url)))

(defn create-paragraph
  "Create paragraph object (outside page loop)"
  [configuration]
  (let [font (PdfFontFactory/createFont
               (FontProgramFactory/createFont FontConstants/HELVETICA))]
    (-> (new Paragraph (:message configuration))
        (.setFont font)
        (.setFontSize (:size configuration)))))

(defn alignment
  "Returns alignment value"
  [configuration]
  (let [alignment (:align configuration)]
  (cond (= alignment "LEFT") TextAlignment/LEFT
        (= alignment "RIGHT") TextAlignment/RIGHT
        :else TextAlignment/CENTER)))

(defn apply-watermark
  "Applys a watermark to a given page of a pdf document"
  [pdf-document document paragraph configuration page-number]
  (let [media-box (-> pdf-document
                      (.getPage page-number)
                      (.getMediaBox))]
    (println (:x-offset configuration) )
    (println (:y-offset configuration))
    (.showTextAligned document
                      paragraph
                      (:x-offset configuration)
                      (:y-offset configuration)
                      page-number
                      (alignment configuration)
                      VerticalAlignment/BOTTOM
                      0)))

(defn watermark-document [output-stream input-pdf-file paragraph configuration]
  (with-open [reader (new PdfReader input-pdf-file)]
    (with-open [pdf-document (new PdfDocument reader (new PdfWriter output-stream))]
      (let [document (new Document pdf-document)]
        (doseq [page-number (range 1 (inc (.getNumberOfPages pdf-document)))]
          (apply-watermark pdf-document
                           document
                           paragraph
                           configuration
                           page-number))))) output-stream)

(defn serve-pdf
  "Takes an encrypted data string, requests a pdf from url and watermarks it. Outputs the pdf"
  [http-request]
  (println (str "Request for " (:uri http-request)))

  (def data-parameter (data/decrypt-http-request http-request))
  (def properties-file (data/load-properties-file (get data-parameter "template")))

  (println properties-file)
  (println data-parameter)

  (try (serve-content "application/pdf"
                      (rio/piped-input-stream
                        (fn [ostream]
                          (watermark-document
                            ostream
                            (stream-file (get data-parameter "url"))
                            (create-paragraph properties-file)
                            properties-file))))
       (catch java.io.IOException ioe
         (println ioe)
         (redirect (get properties-file "url")))))

(defn route [request]
  (cond (= request "") (println "status page pls")
        (= (get request :uri) "favicon.ico") (println "favicon pls")
        (not (nil? (get request :query-string))) (serve-pdf request)
        :else (println "status page pls")))

(defn start [port]
  (println "Initialized watermarker")
  (jetty/run-jetty #'route {:port port}))

(defn -main []
  (let [port (Integer. (or (System/getenv "PORT") "8080"))
        timeout (Integer. (or (System/getenv "TIMEOUT") "25000"))]
    (start port)))
