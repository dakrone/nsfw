(ns nsfw.util
  (:use [hiccup core])
  (:require [clj-stacktrace.repl :as stacktrace]
            [clojure.string :as string]))

(defn web-stacktrace [e req]
  (str "<html><body>"
       "<h1>500 - " (.getMessage e) "</h1>"
       
       "<pre>" (stacktrace/pst-str e) "</pre>"

       "<pre>" (string/replace (str req) #", " "\n") "</pre>"
       "</html></body>"))

(defn include-css
  ([n]
     (when n
       (if (= :all n)
         (->> (file-seq (java.io.File. "./public/css/"))
              (filter #(.endsWith (.getName %) ".css"))
              (filter #(.isFile %))
              (map #(.getName %))
              (map include-css))
         (let [filename (if (keyword? n)
                          (str (name n) ".css")
                          n)]
           (html [:link {:rel "stylesheet" :href (str "/css/" filename) :type "text/css"}])))))
  ([n & more]
     (cons (include-css n) (map include-css more))))

(def *local-js-root* "./public/js")

(defn include-js
  ([n]
     (when n
       (if (= :all n)
         (->> (file-seq (java.io.File. "./public/js/"))
              (filter #(.endsWith (.getName %) ".js"))
              (filter #(.isFile %))
              (map #(.getName %))
              (map include-css))
         (let [filename (if (keyword? n)
                          (str (name n) ".js")
                          n)]
           (html [:script {:type "text/javascript" :src (str "/js/" filename)}])))))
  ([n & more]
     (cons (include-js n) (map include-js more))))

(defn container [width & body]
  (apply vector (concat [:div {:class (str "container_" width)}] body)))

(def container-16 (partial container 16))

(defn grid [width & body]
  (apply vector (concat [:div {:class (str "grid_" width)}] body)))

(def grid-16 (partial grid 16))

(defn image [name & opts]
  (let [opts (apply hash-map opts)
        opts (merge {:src (str "/images/" name)} opts)]
    (html [:img opts])))

(defn as-str [thing]
  (if (keyword? thing)
    (str (name thing))
    (str thing)))

(defn css [& in]
  (apply
   str
   (interpose
    " "
    (map 
     (fn [r]
       (cond
        (vector? r) (let [sels (take (- (count r) 1) r)
              rules (first (reverse r))]
         
          (str (if (vector? (first sels))
                 (apply str (interpose ", " (map #(apply str (interpose " " (map as-str %))) sels)))
                 (apply str (interpose " " (map as-str sels))))
               " {"
               (apply str (map #(str (name (key %)) ":" (as-str (val %)) ";") rules))
               "}"))
        :else r))
     in))))

(defn throw-str [& args]
  (throw (Exception. (apply str (interpose " " args)))))

(defn md5-sum
  "Compute the hex MD5 sum of a string."
  [#^String str]
  (let [alg (doto (java.security.MessageDigest/getInstance "MD5")
              (.reset)
              (.update (.getBytes str)))]
    (try
      (.toString (new BigInteger 1 (.digest alg)) 16)
      (catch java.security.NoSuchAlgorithmException e
        (throw (new RuntimeException e))))))

(defn grav-url-for [email & [_ size]]
  (let [email (->> email
                   (clojure.string/trim)
                   (clojure.string/lower-case))
        url (str "http://gravatar.com/avatar/" (md5-sum email))]
    (if size
      (str url "?s=" size)
      url)))
