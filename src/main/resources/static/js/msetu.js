const mountWeatherScene = (sky, body, reduceMotion) => {
  const icon = body.dataset.weatherIcon || '';
  let code = Number(body.dataset.weatherCode || 0);
  if (!code) {
    const family = icon.slice(0, 2);
    code = ({ '01': 800, '02': 801, '03': 802, '04': 804, '09': 520, '10': 500, '11': 200, '13': 600, '50': 701 })[family] || 0;
  }

  let scene = 'cloudy';
  if (code >= 200 && code <= 232) scene = 'storm';
  else if ((code >= 300 && code <= 321) || (code >= 500 && code <= 531)) scene = 'rain';
  else if (code >= 600 && code <= 622) scene = 'snow';
  else if (code >= 701 && code <= 781) scene = 'mist';
  else if (code === 800) scene = 'clear';
  else if (code === 801 || code === 802) scene = 'partly-cloudy';
  else if (code === 803 || code === 804) scene = 'overcast';

  sky.dataset.scene = scene;
  sky.classList.add('effect-' + scene);
  const isNight = body.dataset.isDaylight === 'false' && body.dataset.weatherCode !== '0';
  if (isNight) sky.classList.add('time-night');
  const stormHasRain = [200, 201, 202, 230, 231, 232].includes(code);
  const precipRate = Number.parseFloat(body.dataset.precipitationRate);
  const hasReportedRain = Number.isFinite(precipRate) && precipRate > 0;
  const hasRain = scene === 'rain' || (scene === 'storm' && (stormHasRain || hasReportedRain));
  if (hasRain) {
    sky.classList.add('has-rain');
    const intensity = !Number.isFinite(precipRate) || precipRate < 2.5 ? 'light'
      : precipRate < 7.5 ? 'moderate' : 'heavy';
    sky.classList.add('rain-' + intensity);
    if (!reduceMotion) {
    const mobile = window.matchMedia('(max-width: 700px)').matches;
    const baseCounts = intensity === 'heavy' ? [48, 30] : intensity === 'moderate' ? [34, 20] : [22, 12];
    const counts = mobile ? baseCounts.map(count => Math.max(8, Math.round(count * .48))) : baseCounts;
    const direction = Number.parseFloat(body.dataset.windDirection);
    const speed = Number.parseFloat(body.dataset.windSpeed) || 0;
    const toward = Number.isFinite(direction) ? (direction + 180) * Math.PI / 180 : Math.PI / 3;
    const windDrift = Math.max(-52, Math.min(52, Math.sin(toward) * (10 + Math.min(speed, 12) * 3.2)));
    const addRain = (layer, count, foreground) => {
      const fragment = document.createDocumentFragment();
      for (let i = 0; i < count; i++) {
        const drop = document.createElement('span');
        const duration = foreground ? .58 + Math.random() * .62 : 1.25 + Math.random() * 1.05;
        const length = foreground ? 13 + Math.random() * 12 : 7 + Math.random() * 9;
        const opacity = foreground ? .28 + Math.random() * .3 : .14 + Math.random() * .2;
        const drift = windDrift * (foreground ? 1 : .62) + (Math.random() - .5) * 8;
        drop.className = 'rain-drop';
        drop.style.setProperty('--drop-x', (Math.random() * 100).toFixed(2) + '%');
        drop.style.setProperty('--drop-delay', (-Math.random() * duration).toFixed(2) + 's');
        drop.style.setProperty('--drop-duration', duration.toFixed(2) + 's');
        drop.style.setProperty('--drop-length', length.toFixed(1) + 'px');
        drop.style.setProperty('--drop-opacity', opacity.toFixed(2));
        drop.style.setProperty('--wind-drift', drift.toFixed(1) + 'px');
        drop.style.setProperty('--drop-angle', (Math.atan(drift / 420) * 180 / Math.PI).toFixed(1) + 'deg');
        fragment.append(drop);
      }
      layer.replaceChildren(fragment);
    };
    addRain(sky.querySelector('.sky-rain-back'), counts[0], false);
    addRain(sky.querySelector('.sky-rain-front'), counts[1], true);
    }
  }
  if (isNight && code === 800) sky.classList.add('clear-night');
  if (scene === 'snow' && !reduceMotion) {
    const snow = sky.querySelector('.sky-snow');
    const fragment = document.createDocumentFragment();
    const count = window.matchMedia('(max-width: 700px)').matches ? 16 : 32;
    for (let i = 0; i < count; i++) {
      const flake = document.createElement('span');
      const duration = 10 + Math.random() * 12;
      flake.className = 'snow-flake';
      flake.style.setProperty('--flake-x', (Math.random() * 100).toFixed(2) + '%');
      flake.style.setProperty('--flake-delay', (-Math.random() * duration).toFixed(2) + 's');
      flake.style.setProperty('--flake-duration', duration.toFixed(2) + 's');
      flake.style.setProperty('--flake-size', (1.5 + Math.random() * 3).toFixed(1) + 'px');
      flake.style.setProperty('--flake-drift', ((Math.random() - .5) * 42).toFixed(1) + 'px');
      flake.style.setProperty('--flake-opacity', (.3 + Math.random() * .5).toFixed(2));
      fragment.append(flake);
    }
    snow?.replaceChildren(fragment);
  }

  const drift = sky.querySelector('.sky-drift');
  const sceneImage = scene === 'clear' ? '/images/clear-sky.png'
    : ['rain', 'storm'].includes(scene) ? '/images/storm-sky.png' : null;
  const removeParticles = () => sky.querySelectorAll('.rain-drop, .snow-flake').forEach(particle => particle.remove());
  if (!drift || !sceneImage) return removeParticles;
  if (reduceMotion) {
    drift.src = sceneImage;
    sky.classList.add('image-ready');
    return () => {};
  }

  const preload = new Image();
  preload.onload = () => {
    drift.src = sceneImage;
    sky.classList.add('image-ready');
  };
  preload.onerror = () => sky.classList.add('image-fallback');
  preload.src = sceneImage;
  return () => {
    preload.onload = null;
    preload.onerror = null;
    preload.src = '';
    sky.querySelectorAll('.rain-drop, .snow-flake').forEach(particle => particle.remove());
  };
};

document.addEventListener('DOMContentLoaded', () => {
  const lifetime = new AbortController();
  const { signal } = lifetime;
  const root = document.documentElement;
  root.classList.add('has-motion-js');

  const toggle = document.querySelector('.nav-toggle');
  const links = document.querySelector('.nav-links');
  toggle?.addEventListener('click', () => {
    const open = toggle.getAttribute('aria-expanded') !== 'true';
    toggle.setAttribute('aria-expanded', String(open));
    toggle.setAttribute('aria-label', open ? 'Close navigation' : 'Open navigation');
    links?.classList.toggle('open', open);
  }, { signal });
  links?.querySelectorAll('a').forEach(link => link.addEventListener('click', () => {
    links.classList.remove('open');
    toggle?.setAttribute('aria-expanded', 'false');
    toggle?.setAttribute('aria-label', 'Open navigation');
  }, { signal }));

  const unitButton = document.getElementById('unit-toggle');
  let fahrenheit = false;
  unitButton?.addEventListener('click', () => {
    fahrenheit = !fahrenheit;
    document.querySelectorAll('[data-temp-c]').forEach(element => {
      const celsius = Number(element.dataset.tempC);
      element.textContent = (fahrenheit ? celsius * 9 / 5 + 32 : celsius).toFixed(0);
    });
    document.querySelectorAll('[data-temp-unit]').forEach(element => element.textContent = fahrenheit ? '°F' : '°C');
    unitButton.textContent = fahrenheit ? 'Show °C' : 'Show °F';
    unitButton.setAttribute('aria-label', fahrenheit ? 'Switch temperatures to Celsius' : 'Switch temperatures to Fahrenheit');
  }, { signal });

  document.querySelectorAll('form.location-search, form[action="/weather"]').forEach(form => form.addEventListener('submit', () => {
    const button = form.querySelector('button[type="submit"]');
    if (!button) return;
    button.dataset.originalLabel = button.textContent;
    button.textContent = 'Finding weather';
    button.setAttribute('aria-busy', 'true');
    button.disabled = true;
    form.setAttribute('aria-busy', 'true');
    document.body.classList.add('searching');
  }, { signal }));

  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  let entranceFrameOne = 0;
  let entranceFrameTwo = 0;
  if (reduceMotion) document.body.classList.add('page-entered');
  else {
    entranceFrameOne = requestAnimationFrame(() => {
      entranceFrameTwo = requestAnimationFrame(() => document.body.classList.add('page-entered'));
    });
  }
  let observer = null;
  const reveal = document.querySelectorAll('.reveal-section');
  if ('IntersectionObserver' in window && !reduceMotion) {
    observer = new IntersectionObserver(entries => entries.forEach(entry => {
      if (!entry.isIntersecting) return;
      entry.target.classList.add('is-visible');
      observer.unobserve(entry.target);
    }), { threshold: 0.12 });
    reveal.forEach(section => observer.observe(section));
  } else reveal.forEach(section => section.classList.add('is-visible'));

  const sky = document.querySelector('.atmospheric-sky');
  const clearScene = sky ? mountWeatherScene(sky, document.body, reduceMotion) : () => {};
  const updateVisibility = () => document.body.classList.toggle('page-hidden', document.hidden);
  document.addEventListener('visibilitychange', updateVisibility, { signal });
  updateVisibility();

  document.querySelectorAll('.temperature-dot').forEach((dot, index) => dot.style.setProperty('--dot-order', String(index)));
  window.addEventListener('pagehide', event => {
    if (event.persisted) return;
    observer?.disconnect();
    clearScene();
    cancelAnimationFrame(entranceFrameOne);
    cancelAnimationFrame(entranceFrameTwo);
    lifetime.abort();
  }, { once: true, signal });
});
