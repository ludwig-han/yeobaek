'use strict';
async function copyValue(value, button) {
  try {
    await navigator.clipboard.writeText(value);
    const original=button.textContent; button.textContent='복사했어요';
    window.setTimeout(()=>{button.textContent=original;},2000);
  } catch (_) {
    const box=document.getElementById('copy-fallback');
    const field=document.getElementById('copy-value');
    if(box && field){box.hidden=false;field.value=value;field.focus();field.select();}
  }
}
document.querySelectorAll('[data-copy-input]').forEach(button=>{
  button.addEventListener('click',()=>copyValue(document.getElementById(button.dataset.copyInput).value,button));
});
document.querySelectorAll('[data-share-path]').forEach(button=>{
  button.addEventListener('click',()=>copyValue(new URL(button.dataset.sharePath,window.location.origin).href,button));
});
document.querySelectorAll('form').forEach(form=>{
  form.addEventListener('submit',()=>{
    const button=form.querySelector('button[type="submit"]');
    if(button){button.dataset.originalLabel=button.textContent;button.dataset.submitted='true';button.disabled=true;button.textContent=button.dataset.busyLabel || '저장 중…';}
  });
});
window.addEventListener('pageshow',()=>{
  document.querySelectorAll('button[type="submit"]').forEach(button=>{
    if(button.dataset.submitted){button.disabled=false;button.textContent=button.dataset.originalLabel;delete button.dataset.submitted;}
  });
});
if(document.querySelector('[data-research-pending]')) {
  window.setTimeout(()=>window.location.reload(),5000);
}

// Keep credentials on this origin only; never place edit keys in URLs.
(() => {
  const storageKey = 'yeobaek.myPlans.v1';
  const token = /^[A-Za-z0-9_-]{43}$/;
  const list = document.querySelector('[data-library-list]');
  const current = document.querySelector('[data-remember-plan]');
  if (!list && !current) return;
  const failure = () => {
    const notice = document.querySelector('[data-library-error]');
    if (notice) notice.hidden = false;
    else {
      const p = document.createElement('p');
      p.className = 'notice error';
      p.setAttribute('role', 'status');
      p.textContent = '이 브라우저에 계획 목록을 보관하지 못했어요. 계획 주소와 수정 키를 따로 보관해주세요.';
      document.querySelector('main').prepend(p);
    }
  };
  try {
    const raw = localStorage.getItem(storageKey);
    const parsed = raw ? JSON.parse(raw) : [];
    if (!Array.isArray(parsed)) throw new Error('Invalid library');
    let plans = parsed.filter(p => p && token.test(p.id) && token.test(p.key) &&
      typeof p.title === 'string' && typeof p.date === 'string' && typeof p.region === 'string');
    if (current) {
      const data = current.dataset;
      const existing = plans.find(p => p.id === data.id);
      const key = token.test(data.key || '') ? data.key : existing?.key;
      if (token.test(data.id) && token.test(key || '')) {
        plans = plans.filter(p => p.id !== data.id);
        plans.unshift({id: data.id, key, title: data.title || '', date: data.date || '', region: data.region || ''});
        localStorage.setItem(storageKey, JSON.stringify(plans));
      }
      current.remove();
    }
    if (list) {
      document.querySelector('[data-library-empty]').hidden = plans.length > 0;
      for (const plan of plans) {
        const item = document.createElement('article');
        item.className = 'library-item';
        const title = document.createElement('h3');
        title.textContent = plan.title;
        const info = document.createElement('p');
        info.className = 'hint';
        info.textContent = [plan.date, plan.region].filter(Boolean).join(' · ');
        const form = document.getElementById('library-open-form').content.firstElementChild.cloneNode(true);
        form.action = '/p/' + plan.id + '/unlock';
        form.querySelector('[name="editKey"]').value = plan.key;
        form.querySelector('button').setAttribute('aria-label', plan.title + ' 계획 열기');
        item.append(title, info, form);
        list.append(item);
      }
    }
  } catch (_) { failure(); }
})();
