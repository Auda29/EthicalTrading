const english = document.documentElement.lang === 'en';
const selector = document.querySelector('[data-download-selector]');
if (selector) {
  const options = JSON.parse(document.querySelector('#release-options').textContent);
  const game = document.querySelector('#game');
  const loader = document.querySelector('#loader');
  const update = () => {
    const chosen = options.find(item => item.game === game.value && item.loader === loader.value);
    if (!chosen) return;
    document.querySelector('[data-selected-download]').href = chosen.url;
    document.querySelector('[data-requirements]').textContent = `Java ${chosen.java} · ${chosen.loader} ${chosen.minimum}+`;
  };
  game.addEventListener('change', update);
  loader.addEventListener('change', update);
  update();
  selector.hidden = false;
}
const sleepControl = document.querySelector('[data-sleep-control]');
if (sleepControl) {
  const input = document.querySelector('#missed-nights');
  const output = document.querySelector('[data-sleep-output]');
  const quantities = [100, 100, 80, 60, 40, 20, 0];
  const update = () => {
    const nights = Number(input.value);
    const quantity = quantities[nights];
    const unit = english ? (nights === 1 ? 'night-equivalent' : 'night-equivalents') : (nights === 1 ? 'Nachtäquivalent' : 'Nachtäquivalente');
    const status = nights < 2 ? (english ? 'normal trading' : 'normaler Handel') : nights < 6 ? (english ? 'restricted' : 'eingeschränkt') : (english ? 'strike' : 'Streik');
    const text = `${nights}${nights === 6 ? '+' : ''} ${unit} · ${quantity}% · ${status}`;
    output.textContent = text;
    input.setAttribute('aria-valuetext', text);
    document.querySelectorAll('[data-night]').forEach(column => column.classList.toggle('selected', Number(column.dataset.night) === nights));
  };
  input.addEventListener('input', update);
  update();
  sleepControl.hidden = false;
}
