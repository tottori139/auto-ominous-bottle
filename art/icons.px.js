// Mod GUI icons with the Ominous Bottle mascot. Every frame is a 16x16 char grid built in JS,
// then outlined: transparent pixels reachable from the edge that touch art become "O".
// "," marks see-through glass inside a vessel: drawn transparent, but never outlined.
pal({
  O: "#140c1c", o: "#140c1c80", q: "#140c1c80",       // outline: solid, faded (q = o, keeps off.png at 16 colours)
  b: "#7e453c", c: "#d09476",                          // cork
  e: "#4ac8d5", d: "#3a787f", f: "#1e4752", n: "#132530", // glass
  h: "#a26298", i: "#493348", j: "#231f2a", k: "#0c0619", // ominous liquid
  r: "#a8002f", R: "#ff0048", Q: "#ff7a9c", s: "#7a0026", // eyes: dark, bright, hot, closed lid
  p: "#e2a8da",                                        // bubbles
  T: "#8fe3ff",                                        // tear
  W: "#ffffff", w: "#ffffff99", v: "#ffffff44",        // white: solid, faded, more faded
  X: "#e8283c", x: "#e8283c55",                        // red X: solid, faded
  // grey (off)
  B: "#5a5a5a", C: "#a0a0a0", E: "#b8b8b8", D: "#6e6e6e", F: "#444444", N: "#444444",
  H: "#8a8a8a", I: "#4c4c4c", J: "#2c2c2c", K: "#161616", Z: "#a0a0a0",
  // illager
  G: "#b4b4b4", g: "#6f6f6f", u: "#140c1c",          // brow = outline colour
  // iron sword
  L: "#ffffff", M: "#8e8e8e", P: "#7e453c", "0": "#132530", // reuse white, cork, glass shadow
  // gold + sand
  Y: "#ffd84a", y: "#c8901e", S: "#f3e2a8", A: "#d4bb78", a: "#bfe3ea",
  // bread
  l: "#ebc070", m: "#c98a3a", t: "#8a5420", U: "#9c6224", V: "#6b3f14", "8": "#f6e2b0",
  z: "#ebc070", "9": "#8a5420",                        // crumbs
  // toggles
  "1": "#86e35e", "2": "#3fae3a", "3": "#1f7a2a", "4": "#ff7f6e", "5": "#d93a3a", "6": "#8f1f24",
})

// ---------- grid helpers ----------
const blank = () => Array.from({ length: 16 }, () => Array(16).fill("."))
const clone = (g) => g.map((r) => r.slice())
const rowsOf = (s) => s.split("\n").map((r) => r.trim()).filter((r) => r.length)
function draw(g, s, x = 0, y = 0) {
  rowsOf(s).forEach((row, j) => [...row].forEach((ch, i) => set(g, x + i, y + j, ch)))
  return g
}
function set(g, x, y, ch) {
  if (ch === "." || x < 0 || y < 0 || x > 15 || y > 15) return
  g[y][x] = ch === "_" ? "." : ch
}
const at = (g, x, y) => (x < 0 || y < 0 || x > 15 || y > 15 ? "." : g[y][x])
const FADE = { w: "o", v: "q" }                     // faded art gets a faded outline
const NO_OUTLINE = new Set([..."zp9LMP0Xx"])        // crumbs, bubbles, swords, red X stay bare
function outline(g) {
  const out = new Set(), stack = []
  for (let i = 0; i < 16; i++) stack.push([i, 0], [i, 15], [0, i], [15, i])
  while (stack.length) {
    const [x, y] = stack.pop(), id = y * 16 + x
    if (x < 0 || y < 0 || x > 15 || y > 15 || out.has(id) || g[y][x] !== ".") continue
    out.add(id); stack.push([x + 1, y], [x - 1, y], [x, y + 1], [x, y - 1])
  }
  const res = clone(g)
  for (const id of out) {
    const x = id % 16, y = (id / 16) | 0
    const nb = [at(g, x + 1, y), at(g, x - 1, y), at(g, x, y + 1), at(g, x, y - 1)]
      .filter((ch) => ch !== "." && ch !== "," && !NO_OUTLINE.has(ch))
    if (nb.length) res[y][x] = nb.every((ch) => FADE[ch]) ? FADE[nb[0]] : "O"
  }
  return res
}
const paint = (g) => (c) => {
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) if (g[y][x] !== "." && g[y][x] !== ",") c.px(x, y, g[y][x])
}
const frames = (list) => list.map((g) => frame(paint(outline(g))))
function icon(name, list) {
  anim(name + "_anim", { w: 16, h: 16, fps: 4 }, ...frames(list))
  sheet(name, [name + "_anim"], { cols: 1, pad: 0 })
}

// ---------- the mascot (vanilla ominous_bottle texture) ----------
const BOTTLE = `
  ................
  ................
  ......bccb......
  .....deeddf.....
  ......nbbn......
  ......d,,f......
  ......d,,f......
  .....e,,,,d.....
  .....ehhhid.....
  ....eijiijid....
  ....ejkkjjid....
  ...djrRijRrje...
  ...dfkijkkkfe...
  ....dfjkkjfe....
  .....ffffdd.....
  ................`
const bottle = () => draw(blank(), BOTTLE)
const LIQUID = new Set([..."hijkrR"])
const eyes = (g, row11) => draw(g, row11, 3, 11)   // replace the eye row (x3..12)
const NO_EYES = "djjkijkjje"

// 1. on: slow blink
icon("on", [
  bottle(),
  eyes(bottle(), "djrrijrrje"),
  eyes(bottle(), "djssijssje"),
  eyes(bottle(), "djrrijrrje"),
])

// 2. off: grey, eyes closed, a "z" floats up and fades
const GREY = { b: "B", c: "C", e: "E", d: "D", f: "F", n: "N", h: "H", i: "I", j: "J", k: "K" }
const greyBottle = () => eyes(bottle(), "djZZijZZje").map((r) => r.map((ch) => GREY[ch] ?? ch))
const zz = (ch) => [ch + ch + ".", "." + ch + ".", "." + ch + ch].join("\n")
icon("off", [
  draw(greyBottle(), zz("W"), 12, 5),
  draw(greyBottle(), zz("W"), 12, 3),
  draw(greyBottle(), zz("w"), 13, 1),
  draw(greyBottle(), zz("v"), 13, 0),
])

// 3. omen: hot eyes, pulsing illager face (Bad Omen) top-right
const ILL3 = "uuu\nGgG\n.g."
const ILL4 = "GGGG\nuuuu\nGggG\n.gg."
const ILL5 = ".GGG.\nGuuuG\nGGgGG\n.GgG.\n..g.."   // unibrow + long nose; narrow chin keeps it off the rim
const omen = () => eyes(bottle(), "djRQijQRje")
icon("omen", [
  draw(omen(), ILL3, 12, 1),
  draw(omen(), ILL4, 12, 0),
  draw(omen(), ILL5, 11, 0),
  draw(omen(), ILL4, 12, 0),
])

// 4. raid: angry eyes, two iron swords behind the bottle clash above the cork
function sword(g, tx, ty, dir) {          // dir 1: tip up-right (left sword), -1: mirrored
  const p = (dx, dy, ch) => set(g, tx + dx * dir, ty + dy, ch)
  p(0, 0, "L"); p(-1, 1, "L"); p(-2, 2, "L"); p(-3, 3, "L")          // blade
  p(0, 1, "M"); p(-1, 2, "M"); p(-2, 3, "M")                        // blade shadow
  p(-5, 3, "0"); p(-4, 4, "0"); p(-3, 5, "0")                       // guard
  p(-5, 5, "P"); p(-6, 6, "0")                                      // grip, pommel
  return g
}
const raid = (gap) => {
  const g = blank()
  sword(g, 7 - gap, 0, 1); sword(g, 8 + gap, 0, -1)
  draw(g, BOTTLE)
  eyes(g, "djjRijRjje")
  set(g, 5, 10, "r"); set(g, 10, 10, "r")          // outer corners up: angry slant
  if (!gap) { set(g, 7, 0, "W"); set(g, 8, 0, "W"); set(g, 6, 0, "W"); set(g, 9, 0, "W") }
  return g
}
icon("raid", [raid(1), raid(0), raid(1), raid(0)])

// 5. wait: hourglass, sand runs down, then flipped
const GLASS = `
  ................
  ..YYYYYYYYYYYY..
  ..yyyyyyyyyyyy..
  ...a,,,,,,,,a...
  ...a,,,,,,,,a...
  ....a,,,,,,a....
  .....a,,,,a.....
  ......a,,a......
  ......a,,a......
  .....a,,,,a.....
  ....a,,,,,,a....
  ...a,,,,,,,,a...
  ...a,,,,,,,,a...
  ..YYYYYYYYYYYY..
  ..yyyyyyyyyyyy..
  ................`
const fillRow = (g, y, ch) => { for (let x = 0; x < 16; x++) if (g[y][x] === ",") g[y][x] = ch }
function hourglass(topFrom, botFrom, stream) {
  const g = draw(blank(), GLASS)
  for (let y = topFrom; y <= 7; y++) fillRow(g, y, y === topFrom ? "S" : "A")
  for (let y = botFrom; y <= 12; y++) fillRow(g, y, y === botFrom ? "S" : "A")
  for (let y = 8; y < stream; y++) g[y][7] = "S"
  return g
}
const hgEnd = hourglass(99, 9, 0)
icon("wait", [
  hourglass(4, 99, 12),
  hourglass(5, 12, 12),
  hourglass(6, 11, 11),
  hourglass(7, 10, 10),
  hgEnd,
  hgEnd.slice().reverse().map((r) => r.slice()),
])

// 6. drinking: no cork, tilts right, liquid drains, bubbles rise, happy eyes at the end
function drinking(k) {
  const tilt = [0, 0.5, 1, 1.5, 2, 2.5][k], level = [8, 8, 9, 9, 10, 11][k]
  const src = bottle()
  for (let x = 6; x <= 9; x++) { src[2][x] = "."; if (src[4][x] === "b") src[4][x] = "," }   // uncorked
  for (let y = 8; y < level; y++) for (let x = 0; x < 16; x++) if (LIQUID.has(src[y][x])) src[y][x] = ","
  for (let x = 0; x < 16; x++) if (LIQUID.has(src[level][x])) src[level][x] = "h"
  const happy = k >= 4
  if (happy) { if (level < 11) eyes(src, NO_EYES); else for (const x of [5, 6, 9, 10]) src[11][x] = "h" }
  else eyes(src, "djrRijRrje")
  const bubbles = [[[7, 13]], [[7, 12], [9, 13]], [[7, 11], [9, 12]], [[8, 12], [9, 11]], [[7, 13]], [[9, 13]]][k]
  for (const [x, y] of bubbles) if (y > level && LIQUID.has(src[y][x])) src[y][x] = "p"
  const g = blank(), shift = (y) => Math.round((tilt * Math.max(0, 14 - y)) / 11)
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) set(g, x + shift(y), y, src[y][x])
  // happy ^ ^ eyes go on after the tilt so the skew can't break their shape
  if (happy) for (const [x, y] of [[4, 12], [5, 11], [6, 12], [9, 12], [10, 11], [11, 12]]) set(g, x + shift(12), y, "R")
  return g
}
icon("drinking", [0, 1, 2, 3, 4, 5].map(drinking))

// 7. eating: vanilla-style bread loaf, bitten from the right
const BREAD = `
  ................
  ................
  ................
  ................
  ................
  ....llllllll....
  ..llmmlmmlmmll..
  .lmmtmmtmmtmmml.
  .mmmmtmmtmmtmmm.
  .UmmmmmmmmmmmmU.
  .UUUUUUUUUUUUUU.
  ..VVVVVVVVVVVV..
  ................
  ................
  ................
  ................`
function bread(cut, crumbs = []) {
  const g = draw(blank(), BREAD)
  for (let y = 5; y <= 11; y++) {
    const from = cut[y - 5]
    for (let x = from; x < 16; x++) g[y][x] = "."
    if (from < 15 && g[y][from - 1] !== ".") g[y][from - 1] = "8"   // soft inside at the bite
  }
  for (const [x, y, ch] of crumbs) set(g, x, y, ch)
  return g
}
icon("eating", [
  bread([99, 99, 99, 99, 99, 99, 99]),
  bread([12, 13, 12, 12, 12, 13, 13]),
  bread([9, 9, 8, 8, 8, 9, 10]),
  bread([6, 5, 5, 5, 5, 5, 6], [[8, 12, "z"], [10, 13, "9"], [12, 12, "z"], [9, 11, "9"], [11, 13, "z"]]),
])

// 8. no_bottles: empty glass, sad eyes looking down, blinking red X, a tear
function empty(xKey, tear) {
  const g = bottle()
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) if (LIQUID.has(g[y][x])) g[y][x] = ","
  set(g, 6, 9, "W"); set(g, 5, 10, "W")                         // glass glint
  for (const [x, y, ch] of [[5, 12, "r"], [6, 11, "R"], [9, 11, "R"], [10, 12, "r"]]) set(g, x, y, ch)
  if (tear) set(g, 5, tear, "T")
  draw(g, [xKey + "." + xKey, "." + xKey + ".", xKey + "." + xKey].join("\n"), 13, 13)
  return g
}
icon("no_bottles", [empty("X"), empty("x", 13), empty("X", 14), empty("x")])

// 9-10. toggles: rounded plate with a white check / X
function plate(hi, mid, lo, mark) {
  const g = blank()
  for (let y = 1; y <= 14; y++) for (let x = 1; x <= 14; x++) g[y][x] = mid
  for (let x = 2; x <= 13; x++) { g[1][x] = hi; g[14][x] = lo }
  for (let y = 2; y <= 13; y++) { g[y][1] = hi; g[y][14] = lo }
  for (const [x, y] of [[1, 1], [14, 1], [1, 14], [14, 14]]) g[y][x] = "."
  for (const [x, y] of mark) set(g, x, y + 1, lo)                 // drop shadow
  for (const [x, y] of mark) set(g, x, y, "W")
  return g
}
const CHECK = [[11, 4], [10, 5], [11, 5], [9, 6], [10, 6], [8, 7], [9, 7], [4, 7], [4, 8], [5, 8], [7, 8], [8, 8], [5, 9], [6, 9], [7, 9], [6, 10]]
const CROSS = []
for (let i = 0; i <= 6; i++) CROSS.push([4 + i, 4 + i], [5 + i, 4 + i], [11 - i, 4 + i], [10 - i, 4 + i])
icon("toggle_on", [plate("1", "2", "3", CHECK)])
icon("toggle_off", [plate("4", "5", "6", CROSS)])
