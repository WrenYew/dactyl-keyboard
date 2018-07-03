;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Dactyl-ManuForm Keyboard — Opposable Thumb Edition              ;;
;; Miscellaneous CAD Utilities                                         ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; Functions useful in more than one scad-clj project.

(ns dactyl-keyboard.cad.misc
  (:require [unicode-math.core :refer [π √]]
            [scad-clj.model :exclude [use import] :refer :all]))

(defn- supported-threaded-fastener [mapping]
  (fn [size]
    (let [value (get mapping size)]
      (if (nil? value)
        (do (println (format "Unsupported threaded fastener dimension: ‘%s’." size))
            (System/exit 1))
        value))))

(def iso-hex-nut-flat-to-flat
  "A map of ISO screw diameter to hex nut width in mm.
  This is measuring flat to flat (i.e. short diagonal).
  Actual nuts tend to be a little smaller, in which case these standard
  sizes are good for a very tight fit in 3D printing, after accounting for
  printer inaccuracy and material shrinkage."
  (supported-threaded-fastener {3 5.5
                                4 7
                                5 8
                                6 10
                                8 13}))

(def iso-hex-nut-height
  "A map of ISO screw diameter to (maximum) hex nut height."
  (supported-threaded-fastener {3 2.4
                                4 3.2
                                5 4.7
                                6 5.2
                                8 6.8}))

(defn iso-hex-nut-diameter [iso-size]
  "A formula for hex diameter (long diagonal)."
  (* 2 (/ (iso-hex-nut-flat-to-flat iso-size) (√ 3))))

(defn iso-hex-nut-model
  "A model of a hex nut for a boss or pocket. No through-hole."
  ([iso-size]
   (iso-hex-nut-model iso-size (iso-hex-nut-height iso-size)))
  ([iso-size height]
   (rotate [0 0 (/ π 6)]
     (with-fn 6
       (cylinder (/ (iso-hex-nut-diameter iso-size) 2) height)))))

(defn countersink [d length]
  "A negative for a flat-head screw of diameter d."
  (let [r (/ d 2)]
    (union
      (cylinder d 1)
      (translate [0 0 (/ r -2)]
        (cylinder [r d] r))
      (translate [0 0 (/ length -2)]
        (cylinder r length)))))

(defn pairwise-hulls [& shapes]
  (apply union (map (partial apply hull) (partition 2 1 shapes))))

(defn bottom-extrusion [height p]
  (->> (project p)
       (extrude-linear {:height height :twist 0 :convexity 0 :center false})))

(defn bottom-hull [& p]
  (hull p (bottom-extrusion 0.001 p)))

(defn swing-callables [translator radius rotator obj]
  "Rotate passed object with passed radius, not around its own axes.
  The ‘translator’ function receives a vector based on the radius, in the z
  axis only, and an object to translate.
  If ‘rotator’ is a 3-vector of angles or a 2-vector of an angle and an axial
  filter, a rotation function will be created based on that."
  (if (vector? rotator)
    (if (= (count rotator) 3)
      (swing-callables translator radius (partial rotate rotator) obj)
      (swing-callables translator radius
        (partial rotate (first rotator) (second rotator))
        obj))
    ;; Else assume the rotator is usable as a function and apply it.
    (->> obj
      (translator [0 0 (- radius)])
      rotator
      (translator [0 0 radius]))))
