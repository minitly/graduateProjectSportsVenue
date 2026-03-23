import { Ft as onMounted, Ot as nextTick, Vt as openBlock, nt as defineComponent, q as createElementBlock, zt as onUnmounted } from "./vue.runtime.esm-bundler-DWDfmXPD.js";
import { t as tsParticles } from "./browser-pNiIJbRV.js";
//#region node_modules/.pnpm/@tsparticles+vue3@3.0.1/node_modules/@tsparticles/vue3/dist/particles.es.js
var _ = ["id"], w = /* @__PURE__ */ defineComponent({
	__name: "vue-particles",
	props: {
		id: {},
		options: {},
		url: {}
	},
	emits: ["particlesLoaded"],
	setup(r, { emit: e }) {
		let t, i;
		const n = r, c = e;
		addEventListener("particlesInit", (s) => {
			i = s.detail, a();
		});
		const a = async () => {
			i || (i = tsParticles), t = await i.load({
				id: n.id,
				url: n.url,
				options: n.options
			}), c("particlesLoaded", t);
		};
		return onMounted(() => {
			nextTick(() => {
				if (!n.id) throw new Error("Prop 'id' is required!");
				a();
			});
		}), onUnmounted(() => {
			t && (t.destroy(), t = void 0);
		}), (s, d) => (openBlock(), createElementBlock("div", { id: s.id }, null, 8, _));
	}
}), P = (r, e) => {
	r.component("vue-particles", w), (async () => (tsParticles.init(), e.init && await e.init(tsParticles), dispatchEvent(new CustomEvent("particlesInit", { detail: tsParticles }))))();
};
//#endregion
export { P as default };

//# sourceMappingURL=@tsparticles_vue3.js.map