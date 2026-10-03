# Blockbench source snapshot

This directory preserves the model and six PNG files supplied in the 2026-10-03 attachment. `model (4).json` is the source file as received; in it, the outer flame and core flame both refer to `block/texture5`, because the source ZIP contains a duplicate named `texture5 (1).png`.

For an unambiguous import, `rocketengine_source.json` changes only texture slot `5` to `block/texture6`. `texture6.png` is an exact copy of `texture5 (1).png`; `texture5.png` remains the outer flame. The game runtime model files are under `src/main/resources/assets/rocketengine/`.
