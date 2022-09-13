(ns clj-comments.core
  (:require
   [clojure.string :as str]
   [clojure.java.io :as io]
   [clojure.xml :as xml])
  (:import
   java.util.zip.GZIPInputStream
   java.net.URI))


(defn group-tags [xml-nodes]
  (reduce
   (fn [result xml-node]
     (assoc result (:tag xml-node) xml-node))
   {}
   xml-nodes))


(defn group-by-id [maps]
  (reduce
   (fn [result map]
     (assoc result (:id map) map))
   {}
   maps))


(defn xml-post? [xml-node]
  (-> xml-node :tag (= :post)))

(defn xml-thread? [xml-node]
  (-> xml-node :tag (= :thread)))


(defn add-slash [line]
  (if (str/ends-with? line "/")
    line
    (str line "/")))


(def re-date
  #"(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})")


(defn drop-liquid-tags [line]
  (str/replace line #"\{\{|\}\}|\{%|%\}" ""))


(defn xml->post [{:keys [attrs content]}]
  (let [tag->node
        (group-tags content)

        created-at
        (-> tag->node :createdAt :content first)

        date-parsed
        (some->> created-at (re-find re-date) rest)

        message
        (some-> tag->node :message :content first drop-liquid-tags)]

    {:id (-> attrs :dsq:id)
     :message message
     :created-at created-at
     :date-parsed date-parsed
     :deleted? (-> tag->node :isDeleted :content first parse-boolean)
     :spam? (-> tag->node :isSpam :content first parse-boolean)
     :thread (-> tag->node :thread :attrs :dsq:id)
     :parent (-> tag->node :parent :attrs :dsq:id)
     :author
     (let [tag->node
           (-> tag->node :author :content group-tags)]
       {:fullname (-> tag->node :name :content first)
        :anonymous? (-> tag->node :isAnonymous :content first parse-boolean)
        :nickname (-> tag->node :username :content first)})}))


(defn xml->thread [{:keys [attrs content]}]
  (let [tag->node
        (group-tags content)

        link
        (some-> tag->node :link :content first)

        path
        (some-> link (URI.) .getPath add-slash)]

    {:id (-> attrs :dsq:id)
     :category (-> tag->node :category :attrs :dsq:id)
     :forum (-> tag->node :forum :content first)
     :link (-> tag->node :link :content first)
     :path path
     :title (-> tag->node :title :content first)}))


(defn convert [disqus-file]
  (let [stream
        (-> disqus-file
            io/file
            io/input-stream
            (GZIPInputStream.))

        xml-nodes
        (:content (xml/parse stream))

        id->thread
        (->> xml-nodes
             (filter xml-thread?)
             (map xml->thread)
             (group-by-id))

        id->post
        (->> xml-nodes
             (filter xml-post?)
             (map xml->post)
             (group-by-id))]

    (vec
     (for [[post-id post] id->post]
       (let [thread
             (or
              (get id->thread (:thread post))
              (throw (ex-info "thread not found" {:post post})))]
         (assoc post :path (:path thread)))))))


(defn save-post [post-map]

  (let [{:keys [path
                parent
                deleted?
                date-parsed
                author
                id
                spam?
                created-at
                message]}
        post-map

        {:keys [fullname
                nickname
                anonymous?]}
        author

        [year month day hour minute sec]
        date-parsed


        file-path
        (format "../_comments/%s-%s-%s-%s-%s-%s.md"
                year month day hour minute sec)

        file-content
        (with-out-str
          (println "---")
          (println "id:" id)
          (println "is_spam:" spam?)
          (println "is_deleted:" deleted?)
          (println "post:" path)
          (println "date:" created-at)
          (when fullname
            (println "author_fullname:" (format "'%s'" fullname)))
          (when nickname
            (println "author_nickname:" (format "'%s'" nickname)))
          (when (some? anonymous?)
            (println "author_is_anon:" anonymous?))
          (when parent
            (println "parent:" parent))
          (println "---")
          (println)
          (println message))]

    (when (-> file-path io/file .exists)
      (throw (ex-info "file exists" {:file file-path})))

    (when-not (or spam? deleted?)
      (spit file-path file-content))))


#_
(comment

  (def -posts
    (convert "/Users/ivan/work/igrishaev-2022-09-12T10_21_03.597232-all.xml.gz"))

  (mapv save-post -posts)


  (def -xml-post
    {:tag :post,
     :attrs {:dsq:id "5977529195"},
     :content
     [{:tag :id, :attrs nil, :content nil}
      {:tag :message, :attrs nil, :content ["<p>Не знал о таком, посмотрю.</p>"]}
      {:tag :createdAt, :attrs nil, :content ["2022-09-12T06:56:58Z"]}
      {:tag :isDeleted, :attrs nil, :content ["false"]}
      {:tag :isSpam, :attrs nil, :content ["false"]}
      {:tag :author,
       :attrs nil,
       :content
       [{:tag :name, :attrs nil, :content ["Ivan Grishaev"]}
        {:tag :isAnonymous, :attrs nil, :content ["false"]}
        {:tag :username, :attrs nil, :content ["igrishaev"]}]}
      {:tag :thread, :attrs {:dsq:id "9349068282"}, :content nil}
      {:tag :parent, :attrs {:dsq:id "5977195864"}, :content nil}]})

  (def -xml-thread
    {:tag :thread,
     :attrs {:dsq:id "4474349014"},
     :content
     [{:tag :id, :attrs nil, :content nil}
      {:tag :forum, :attrs nil, :content ["igrishaev"]}
      {:tag :category, :attrs {:dsq:id "4340147"}, :content nil}
      {:tag :link, :attrs nil, :content ["http://grishaev.me/2014/11/25/1/"]}
      {:tag :title, :attrs nil, :content ["Впечатления от Мака"]}
      {:tag :message, :attrs nil, :content nil}
      {:tag :createdAt, :attrs nil, :content ["2016-01-08T21:03:58Z"]}
      {:tag :author,
       :attrs nil,
       :content
       [{:tag :name, :attrs nil, :content ["Ivan Grishaev"]}
        {:tag :isAnonymous, :attrs nil, :content ["false"]}
        {:tag :username, :attrs nil, :content ["igrishaev"]}]}
      {:tag :isClosed, :attrs nil, :content ["false"]}
      {:tag :isDeleted, :attrs nil, :content ["false"]}]})

  (xml->post -xml-post)

  (xml->thread -xml-thread)

  )
