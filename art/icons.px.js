// Иконки Auto Ominous Bottle в трёх разрешениях: 16x, 32x, 64x.
// Рисуются в условной сетке 14x14 и масштабируются; outline добавляет по 1px с каждой стороны.
pal({
	k: "#1a1423",
	c: "#9a6638", C: "#5e3a22",
	g: "#d6e6ec", G: "#8fb1bf", H: "#5d7f8f",
	t: "#1f5a52", T: "#3f9a83", y: "#a6f0cf",
	w: "#f4f1e8", a: "#9a9aa3", A: "#5f5f69", d: "#3a3a44",
	r: "#e0474f", R: "#94253a",
	v: "#6fdc5e", V: "#2f8f3c",
	o: "#f3c74e", O: "#a8701d", s: "#ead48c",
	l: "#7fcf4a",
})
ramps("kHGg", "ktTy", "kdAaw", "kRr", "kVv", "kOos")

const SIZES = [16, 32, 64]

for (const size of SIZES) {
	const f = (size - 2) / 14
	const S = (v) => Math.round(v * f)
	const W = size - 2
	const thin = Math.max(1, S(1))

	// Бутылка: пробка, горлышко, круглое тело. liquid — цвета жидкости [тёмный, светлый] или null.
	const bottle = (glass, glassDark, liquid) => [
		ellipse(S(7), S(9.2), S(5), S(4.6), glassDark),
		ellipse(S(7), S(9.2), S(4.2), S(3.9), glass),
		rect(S(5), S(2.2), S(4), S(2.6), glassDark),
		rect(S(5.6), S(2.2), S(2.8), S(2.6), glass),
		rect(S(4.4), S(1.6), S(5.2), Math.max(1, S(1)), glassDark),
		rect(S(5.2), 0, S(3.6), S(1.8), "c"),
		hline(S(5.2), S(1.8) - 1, S(3.6), "C"),
		...(liquid ? [
			clip([0, S(8.2), W, W], ellipse(S(7), S(9.2), S(4.2), S(3.9), liquid[0])),
			clip([0, S(8.2), W, S(1.2)], ellipse(S(7), S(9.2), S(4.2), S(3.9), liquid[1])),
		] : []),
		// блик
		line(S(4), S(7.6), S(4), S(9.6), "w", Math.max(1, S(0.8))),
	]

	const badge = (bg, mark) => [
		circ(S(11), S(11), S(3), "k"),
		circ(S(11), S(11), S(3) - Math.max(1, S(0.6)), bg),
		...mark,
	]
	const check = badge("V", [
		line(S(9.4), S(11), S(10.6), S(12.3), "w", thin),
		line(S(10.6), S(12.3), S(12.7), S(9.8), "w", thin),
	])
	const cross = badge("R", [
		line(S(9.7), S(9.7), S(12.3), S(12.3), "w", thin),
		line(S(12.3), S(9.7), S(9.7), S(12.3), "w", thin),
	])

	const opts = { w: W, h: W, outline: "k" }
	const id = (name) => `icon${size}_${name}`

	sprite(id("on"), opts, ...bottle("G", "H", ["t", "T"]), ...check)
	sprite(id("off"), opts, ...bottle("a", "A", ["d", "A"]))
	sprite(id("no_bottles"), opts, ...bottle("g", "G", null), ...cross)
	// Пузырьки: на 16x одиночные пиксели, крупнее — кружки.
	const bubble = (x, y, r) => size === 16 ? px(S(x), S(y), "y") : circ(S(x), S(y), S(r), "y")
	sprite(id("drinking"), opts, ...bottle("G", "H", ["t", "T"]),
		bubble(8.6, 10.6, 0.7),
		bubble(6.4, 11.8, 0.5),
		bubble(11.6, 4.6, 0.9),
		bubble(12.8, 1.8, 0.6))

	// Скрещённые мечи: идёт рейд.
	const sword = (x0, y0, x1, y1, gx0, gy0, gx1, gy1) => [
		line(S(x0), S(y0), S(x1), S(y1), "g", Math.max(1, S(1.4))),
		line(S(gx0), S(gy0), S(gx1), S(gy1), "o", Math.max(1, S(1))),
	]
	sprite(id("raid"), opts,
		...sword(1, 1, 10, 10, 8, 11.5, 11.5, 8),
		line(S(11), S(11), S(13.2), S(13.2), "C", Math.max(1, S(1.2))),
		...sword(13, 1, 4, 10, 2.5, 8, 6, 11.5),
		line(S(3), S(11), S(0.8), S(13.2), "C", Math.max(1, S(1.2))))

	// Зловещее знамя: ждём начала рейда.
	sprite(id("omen"), opts,
		rect(S(1), 0, S(12), Math.max(1, S(1)), "C"),
		poly([S(2.5), S(1), S(11.5), S(1), S(11.5), S(14), S(7), S(11.5), S(2.5), S(14)], "w"),
		rect(S(2.5), S(2.2), S(9), Math.max(1, S(1)), "d"),
		rect(S(4.5), S(4.5), S(5), S(4.5), "a"),
		rect(S(5.2), S(5.6), S(1.2), Math.max(1, S(0.8)), "d"),
		rect(S(7.6), S(5.6), S(1.2), Math.max(1, S(0.8)), "d"),
		rect(S(5.8), S(7.6), S(2.4), Math.max(1, S(0.6)), "d"),
		shade([S(7), 0, W, W]))

	// Песочные часы: пауза перед следующей бутылкой.
	sprite(id("wait"), opts,
		poly([S(2.6), S(1.6), S(11.4), S(1.6), S(7), S(7)], "G"),
		poly([S(7), S(7), S(11.4), S(12.4), S(2.6), S(12.4)], "G"),
		poly([S(3.4), S(2.2), S(10.6), S(2.2), S(7), S(6.4)], "g"),
		poly([S(7), S(7.6), S(10.6), S(11.8), S(3.4), S(11.8)], "g"),
		poly([S(5), S(3.8), S(9), S(3.8), S(7), S(6.2)], "s"),
		poly([S(7), S(9), S(10.2), S(11.8), S(3.8), S(11.8)], "s"),
		line(S(7), S(6.2), S(7), S(9), "s", Math.max(1, S(0.4))),
		rect(S(1.6), 0, S(10.8), S(1.8), "O"),
		rect(S(1.6), S(12.2), S(10.8), S(1.8), "O"),
		hline(S(1.6), 0, S(10.8), "o"),
		hline(S(1.6), S(12.2), S(10.8), "o"))

	// Яблоко: ест.
	sprite(id("eating"), opts,
		circ(S(5), S(8.6), S(4.4), "r"),
		circ(S(9), S(8.6), S(4.4), "r"),
		shade([S(7), S(14), S(6)]),
		circ(S(4.4), S(7), Math.max(1, S(1)), "w"),
		line(S(7), S(4.4), S(7.6), S(1.6), "C", Math.max(1, S(0.8))),
		ellipse(S(10), S(2.4), S(2.2), S(1.1), "l"))
}

sheet("all", ["icon16_*", "icon32_*", "icon64_*"], { pad: 2 })
