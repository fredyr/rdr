(ns rdr.core
  (:require [goog.net.Jsonp]
            [goog.Uri]
            [goog.string :as gstring]
            [goog.string.format]
            [goog.i18n.DateTimeFormat]
            [goog.date.DateTime]
            [goog.date.Date]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))
(enable-console-print!)

;; * Ideas for improvements
;;
;; - expand blog post for reading
;; - store feed data in local storage
;; - sort listing order by date
;; - favoriting articles you like
;; - read/unread articles
;; - client side routing, https://github.com/gf3/secretary
;; - multimethod rendering of view, https://github.com/swannodette/om/wiki/Tutorial
;; - refresh feed button
;; - infiniscroll https://github.com/guillaumervls/react-infinite-scroll 
;; - pagination (perhaps using routing)
;;

(defn format-date [date-string]
  (let [d (js/Date. date-string)
        format (goog.i18n.DateTimeFormat. "yyyy-MM-dd")]
    (.format format d)))

(defn feed-loader [url cb]
  (let [proxy "https://ajax.googleapis.com/ajax/services/feed/load?v=1.0&num=-1&q="
        req (goog.net.Jsonp. (str proxy url))
        parser (fn [result]
                 (cb (js->clj
                      (.. result -responseData -feed)
                      :keywordize-keys true)))]
    (.send req nil parser)))

(def app-state (atom {:feed-data {:feeds []
                                  :entries []}
                      :showing :all}))

(def feed-list
  ["http://xkcd.com/atom.xml"
   "http://www.lexicallyscoped.com/atom.xml"
   "http://swannodette.github.io/atom.xml"])

(defn prefix-keyword
  [prefix kw]
  (keyword (str prefix (name kw))))

(defn add-feed [app-state feed]
  (let [feed-info (into {} (map (fn [[k v]] [(prefix-keyword "feed-" k) v])
                                (select-keys feed [:title :link :author :description])))
        entries (map #(merge % feed-info) (:entries feed))]
    (-> app-state
        (update-in [:feed-data :feeds] conj feed)
        (update-in [:feed-data :entries] concat entries))
    ))
      
(dorun (map 
        (fn [feed]
          (feed-loader
           feed
           (fn [data] (swap! app-state add-feed data))))
        feed-list))

(defn feed-header
  [{:keys [title author link]} owner]
  (om/component
   (dom/div
    nil
    (dom/h1 nil title)
    (dom/p nil (dom/strong nil author) " " (dom/a nil link)))))

(defn feed-entry
  [{:keys [title link publishedDate contentSnippet stripe] :as entry} owner]
  (om/component
   (dom/tr #js {:className stripe}
           (dom/td nil
                   (dom/span #js {:className "title"} 
                             (dom/a #js {:href link} title)))
           (dom/td nil
                   (dom/span #js {:dangerouslySetInnerHTML #js {:__html contentSnippet}}))
           (dom/td nil
                   (dom/span nil (:feed-title entry)))
           (dom/td nil
                   (dom/span #js {:className "meta no-wrap"} (format-date publishedDate))))))

(defn feed-entries
  [entries owner]
  (let [es (map #(assoc %1 :stripe %2)
                entries
                (cycle ["green-stripe" "white-stripe"]))]
    (om/component
     (dom/table nil
                (dom/thead nil
                           (dom/tr nil
                                   (dom/th nil "Title")
                                   (dom/th nil "Excerpt")
                                   (dom/th nil "Site")
                                   (dom/th nil "Date")))
                (apply dom/tbody nil (om/build-all feed-entry es))))))

(defn feed
  [feed-data owner]
  (om/component
   (dom/div nil
            (om/build feed-header feed-data)
            (om/build feed-entries (:entries feed-data)))))

(defn main
  [{:keys [feed-data] :as state} owner]
  (reify
    om/IWillMount
    (will-mount [_]
      #_(feed-loader
       "http://swannodette.github.io/atom.xml"
       (fn [data] (om/update! feed-data data))))
    om/IRender
    (render [_]
      (om/build feed feed-data))))

(om/root
 main
 app-state
 {:target (.getElementById js/document "content")})
