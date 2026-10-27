/* CantaPagos — interacciones mínimas, sin dependencias */
(() => {
  'use strict';

  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  /* ---------- Año dinámico ---------- */
  const year = document.getElementById('year');
  if (year) year.textContent = new Date().getFullYear();

  /* ---------- Nav con fondo al hacer scroll ---------- */
  const nav = document.getElementById('nav');
  if (nav) {
    const onScroll = () => nav.classList.toggle('is-stuck', window.scrollY > 10);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
  }

  /* ---------- Reveal al entrar en viewport ---------- */
  const items = Array.from(document.querySelectorAll('.reveal'));

  if (reduced || !('IntersectionObserver' in window)) {
    items.forEach((el) => el.classList.add('is-in'));
  } else {
    items.forEach((el) => {
      const siblings = Array.from(el.parentElement.children).filter((n) => n.classList.contains('reveal'));
      const index = siblings.indexOf(el);
      if (siblings.length > 1 && index > 0) {
        el.style.transitionDelay = Math.min(index * 70, 280) + 'ms';
      }
    });

    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-in');
            io.unobserve(entry.target);
          }
        });
      },
      { rootMargin: '0px 0px -8% 0px', threshold: 0.12 }
    );

    items.forEach((el) => io.observe(el));
  }

  /* ---------- Demo de voz + campanita ---------- */
  const demo = document.getElementById('demo');
  const label = document.getElementById('demo-label');
  const PHRASE = '¡Nequi! Recibiste veinticinco mil pesos.';
  let playing = false;

  function chime() {
    const Ctx = window.AudioContext || window.webkitAudioContext;
    if (!Ctx) return Promise.resolve();

    const ctx = new Ctx();
    const now = ctx.currentTime;

    // Dos notas cortas tipo "clin-ck" de caja registradora
    const notes = [
      { t: 0.00, f: 1568.0, g: 0.22 },
      { t: 0.10, f: 2093.0, g: 0.18 }
    ];

    notes.forEach(({ t, f, g }) => {
      [1, 2.01, 3.02].forEach((mult, i) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.value = f * mult;

        const peak = g / (i + 1.6);
        const start = now + t;
        gain.gain.setValueAtTime(0.0001, start);
        gain.gain.exponentialRampToValueAtTime(peak, start + 0.008);
        gain.gain.exponentialRampToValueAtTime(0.0001, start + 0.9 - i * 0.18);

        osc.connect(gain).connect(ctx.destination);
        osc.start(start);
        osc.stop(start + 1);
      });
    });

    return new Promise((resolve) => setTimeout(resolve, 520)).then(() => {
      // Cerrar el contexto cuando el sonido ya terminó
      setTimeout(() => ctx.close().catch(() => {}), 1200);
    });
  }

  function speak(text) {
    return new Promise((resolve, reject) => {
      if (!('speechSynthesis' in window)) {
        reject(new Error('unsupported'));
        return;
      }

      const synth = window.speechSynthesis;
      const utter = new SpeechSynthesisUtterance(text);
      utter.lang = 'es-CO';
      utter.rate = 1;
      utter.pitch = 1.05;
      utter.volume = 1;

      const pickVoice = () => {
        const voices = synth.getVoices() || [];
        const esCo = voices.find((v) => /^es[-_]CO/i.test(v.lang));
        const esAny = voices.find((v) => /^es/i.test(v.lang));
        const chosen = esCo || esAny;
        if (chosen) utter.voice = chosen;
      };

      pickVoice();
      if (!synth.getVoices().length) synth.addEventListener('voiceschanged', pickVoice, { once: true });

      let settled = false;
      const done = () => {
        if (settled) return;
        settled = true;
        resolve();
      };

      utter.onend = done;
      utter.onerror = () => {
        if (settled) return;
        settled = true;
        resolve();
      };

      // Respaldo: algunos navegadores móviles no disparan onend
      setTimeout(done, 6000);

      synth.cancel();
      synth.speak(utter);
    });
  }

  if (demo && label) {
    demo.addEventListener('click', async () => {
      if (playing) return;
      playing = true;
      demo.disabled = true;
      label.textContent = 'Cantando…';

      try {
        await chime();
        await speak(PHRASE);
      } catch (err) {
        label.textContent = 'Vista previa no disponible';
        setTimeout(() => { label.textContent = 'Escuchar cómo canta'; }, 2200);
        playing = false;
        demo.disabled = false;
        return;
      }

      label.textContent = 'Escuchar otra vez';
      playing = false;
      demo.disabled = false;
    });
  }
})();
