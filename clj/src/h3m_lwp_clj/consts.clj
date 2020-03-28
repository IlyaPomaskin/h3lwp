(ns h3m-lwp-clj.consts)

(def ^String settings-name "Settings")
(def ^String atlas-file-name "sprites/h3/all.atlas")
(def ^String edn-file-name "sprites/h3/all.edn")
(def animation-interval 0.180)
(def tile-size 32)


(def tile-types
  {:terrain {:index :terrain-image-index
             :flip-x 0
             :flip-y 1
             :names {0 "dirttl"
                     1 "sandtl"
                     2 "grastl"
                     3 "snowtl"
                     4 "swmptl"
                     5 "rougtl"
                     6 "subbtl"
                     7 "lavatl"
                     8 "watrtl"
                     9 "rocktl"}}
   :river {:index :river-image-index
           :flip-x 2
           :flip-y 3
           :names {1 "clrrvr"
                   2 "icyrvr"
                   3 "mudrvr"
                   4 "lavrvr"}}
   :road {:index :road-image-index
          :flip-x 4
          :flip-y 5
          :names {1 "dirtrd"
                  2 "gravrd"
                  3 "cobbrd"}}})


(def resource
  ["avtwood0.def"
   "avtore0.def"
   "avtsulf0.def"
   "avtmerc0.def"
   "avtcrys0.def"
   "avtgems0.def"
   "avtgold0.def"])


(def dwelling
  {:castle ["avgpike0.def"
            "avgcros0.def"
            "avggrff0.def"
            "avgswor0.def"
            "avgmonk0.def"
            "avgcavl0.def"
            "avgangl0.def"]
   :rampart ["avgcent0.def"
             "avgdwrf0.def"
             "avgelf0.def"
             "avgpega0.def"
             "avgtree0.def"
             "avgunic0.def"
             "avggdrg0.def"]
   :tower ["avggrem0.def"
           "avggarg0.def"
           "avggolm0.def"
           "avgmage0.def"
           "avggeni0.def"
           "avgnaga0.def"
           "avgtitn0.def"]
   :inferno ["avgimp0.def"
             "avggogs0.def"
             "avghell0.def"
             "avgdemn0.def"
             "avgpit0.def"
             "avgefre0.def"
             "avgdevl0.def"]
   :necropolis ["avgskel0.def"
                "avgzomb0.def"
                "avgwght0.def"
                "avgvamp0.def"
                "avglich0.def"
                "avgbkni0.def"
                "avgbone0.def"]
   :dungeon ["avgtrog0.def"
             "avgharp0.def"
             "avgbhld0.def"
             "avgmdsa0.def"
             "avgmino0.def"
             "avgmant0.def"
             "avgrdrg0.def"]
   :stronghold ["avggobl0.def"
                "avgwolf0.def"
                "avgorcg0.def"
                "avgogre0.def"
                "avgrocs0.def"
                "avgcycl0.def"
                "avgbhmt0.def"]
   :fortress ["avggnll0.def"
              "avglzrd0.def"
              "avgdfly0.def"
              "avgbasl0.def"
              "avggorg0.def"
              "avgwyvn0.def"
              "avghydr0.def"]
   :conflux ["avgpixie.def"
             "avgair0.def"
             "avgwatr0.def"
             "avgfire0.def"
             "avgerth0.def"
             "avgelp.def"
             "avgfbrd.def"]})


(def monster
  {1 ["avwpike.def"
      "avwpikx0.def"
      "avwcent0.def"
      "avwcenx0.def"
      "avwgrem0.def"
      "avwgrex0.def"
      "avwimp0.def"
      "avwimpx0.def"
      "avwskel0.def"
      "avwskex0.def"
      "avwtrog0.def"
      "avwinfr.def"
      "avwgobl0.def"
      "avwgobx0.def"
      "avwgnll0.def"
      "avwgnlx0.def"
      "avwpixie.def"
      "avwsprit.def"
      "avwhalf.def"
      "avwpeas.def"]
   2 ["avwlcrs.def"
      "avwhcrs.def"
      "avwdwrf0.def"
      "avwdwrx0.def"
      "avwgarg0.def"
      "avwgarx0.def"
      "avwgog0.def"
      "avwgogx0.def"
      "avwzomb0.def"
      "avwzomx0.def"
      "avwharp0.def"
      "avwharx0.def"
      "avwwolf0.def"
      "avwwolx0.def"
      "avwlizr.def"
      "avwlizx0.def"
      "avwelmw0.def"
      "avwicee.def"
      "avwboar.def"
      "avwrog.def"]
   3 ["avwgrif.def"
      "avwgrix0.def"
      "avwelfw0.def"
      "avwelfx0.def"
      "avwgolm0.def"
      "avwgolx0.def"
      "avwhoun0.def"
      "avwhoux0.def"
      "avwwigh.def"
      "avwwigx0.def"
      "avwbehl0.def"
      "avwbehx0.def"
      "avworc0.def"
      "avworcx0.def"
      "avwdfly.def"
      "avwdfir.def"
      "avwelme0.def"
      "avwstone.def"
      "avwmumy.def"
      "avwnomd.def"]
   4 ["avwswrd0.def"
      "avwswrx0.def"
      "avwpega0.def"
      "avwpegx0.def"
      "avwmage0.def"
      "avwmagx0.def"
      "avwdemn0.def"
      "avwdemx0.def"
      "avwvamp0.def"
      "avwvamx0.def"
      "avwmeds.def"
      "avwmedx0.def"
      "avwogre0.def"
      "avwogrx0.def"
      "avwbasl.def"
      "avwgbas.def"
      "avwelma0.def"
      "avwstorm.def"
      "avwglmg0.def"
      "avwsharp.def"]
   5 ["avwmonk.def"
      "avwmonx0.def"
      "avwtree0.def"
      "avwtrex0.def"
      "avwgeni0.def"
      "avwgenx0.def"
      "avwpitf0.def"
      "avwpitx0.def"
      "avwlich0.def"
      "avwlicx0.def"
      "avwmino.def"
      "avwminx0.def"
      "avwroc0.def"
      "avwrocx0.def"
      "avwgorg.def"
      "avwgorx0.def"
      "avwelmf0.def"
      "avwnrg.def"
      "avwglmd0.def"]
   6 ["avwcvlr0.def"
      "avwcvlx0.def"
      "avwunic0.def"
      "avwunix0.def"
      "avwnaga0.def"
      "avwnagx0.def"
      "avwefre0.def"
      "avwefrx0.def"
      "avwbkni0.def"
      "avwbknx0.def"
      "avwmant0.def"
      "avwmanx0.def"
      "avwcycl0.def"
      "avwcycx0.def"
      "avwwyvr.def"
      "avwwyvx0.def"
      "avwpsye.def"
      "avwmagel.def"
      "avwench.def"]
   7 ["avwangl.def"
      "avwarch.def"
      "avwdrag0.def"
      "avwdrax0.def"
      "avwtitn0.def"
      "avwtitx0.def"
      "avwdevl0.def"
      "avwdevx0.def"
      "avwbone0.def"
      "avwbonx0.def"
      "avwrdrg.def"
      "avwddrx0.def"
      "avwbhmt0.def"
      "avwbhmx0.def"
      "avwhydr.def"
      "avwhydx0.def"
      "avwfbird.def"
      "avwphx.def"]
   10 ["avwfdrg.def"
       "avwazure.def"
       "avwcdrg.def"
       "avwrust.def"]})


(def town
  {:castle "avccasx0.def"
   :rampart "avcramx0.def"
   :tower "avctowx0.def"
   :inferno "avcinfx0.def"
   :necropolis "avcnecx0.def"
   :dungeon "avcdunx0.def"
   :stronghold "avcstrx0.def"
   :fortress "avcftrx0.def"
   :conflux "avchforx.def"})


(def village
  {:castle "avccast0.def"
   :rampart "avcramp0.def"
   :tower "avctowr0.def"
   :inferno "avcinfc0.def"
   :necropolis "avcnecr0.def"
   :dungeon "avcdung0.def"
   :stronghold "avcstro0.def"
   :fortress "avcftrt0.def"
   :conflux "avchfor0.def"})