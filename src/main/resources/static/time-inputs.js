'use strict';
(() => {
  const inputs = [...document.querySelectorAll('[data-time-input]')];
  const mode = document.querySelector('[data-return-mode]');
  function returnFields() {
    if (!mode) return;
    [['[data-return-time]', 'BY_TIME'], ['[data-return-destination]', 'LAST_TRAIN']].forEach(([selector, value]) => {
      const section = document.querySelector(selector);
      section.hidden = mode.value !== value;
      section.querySelectorAll('input').forEach(input => { input.disabled = section.hidden; });
    });
  }
  function validate() {
    inputs.forEach(input => {
      let message = '';
      if (!input.disabled && input.value && !/^([01]\d|2[0-3]):[0-5]\d$/.test(input.value)) message = '24시간제로 입력해주세요. 예: 19:30';
      input.setCustomValidity(message);
    });
    document.querySelectorAll('[data-anchor-times]').forEach(section => {
      const name = section.querySelector('input[name$=".name"]');
      const start = section.querySelector('input[name$=".earliest"]');
      const end = section.querySelector('input[name$=".latest"]');
      const rule = section.querySelector('select[name$=".timeSensitive"]');
      if (!name.value.trim()) return;
      if (start.value && end.value && start.validity.valid && end.validity.valid && start.value >= end.value) {
        end.setCustomValidity('종료 시각은 시작 시각보다 늦어야 해요. 같은 날의 시각을 입력해주세요.');
      } else if (rule.value === 'FIXED_TIME' && !start.value && !end.value) {
        start.setCustomValidity('특정 시각을 선택했어요. 시작 또는 종료 시각을 적어주세요.');
      }
    });
    if (mode?.value === 'BY_TIME') {
      const input = document.querySelector('[data-return-time] input');
      if (!input.value) input.setCustomValidity('귀가 시각을 입력하거나 귀가 기준을 미정으로 바꿔주세요.');
    }
    inputs.forEach(input => {
      const error = document.getElementById(input.getAttribute('aria-describedby'));
      error.textContent = input.disabled ? '' : input.validationMessage;
      input.setAttribute('aria-invalid', String(!input.disabled && !input.validity.valid));
    });
  }
  inputs.forEach((input, index) => {
    const error = document.createElement('span');
    error.id = 'time-error-' + index;
    error.className = 'time-error';
    error.setAttribute('aria-live', 'polite');
    input.setAttribute('aria-describedby', error.id);
    input.parentElement.append(error);
    input.addEventListener('input', validate);
    input.addEventListener('blur', () => {
      const digits = input.value.trim();
      if (/^\d{4}$/.test(digits)) input.value = digits.slice(0, 2) + ':' + digits.slice(2);
      if (/^\d:\d{2}$/.test(digits)) input.value = '0' + digits;
      validate();
    });
  });
  document.querySelectorAll('[data-clear-time]').forEach(button => button.addEventListener('click', () => {
    const input = button.parentElement.querySelector('input');
    input.value = '';
    input.dispatchEvent(new Event('input', {bubbles:true}));
    input.dispatchEvent(new Event('time-cleared'));
    const section = input.closest('[data-anchor-times]');
    if (section && [...section.querySelectorAll('[data-time-input]')].every(i => !i.value)) {
      const rule = section.querySelector('select[name$=".timeSensitive"]');
      if (rule.value === 'FIXED_TIME') rule.value = 'UNKNOWN';
    }
    if (input.closest('[data-return-time]')) { mode.value = 'UNKNOWN'; returnFields(); mode.focus(); }
    else input.focus();
    validate();
  }));
  document.querySelectorAll('[data-anchor-times] select, [data-anchor-times] input[name$=".name"]').forEach(input => input.addEventListener('change', validate));
  mode?.addEventListener('change', () => { returnFields(); validate(); });
  returnFields();
  validate();
})();
