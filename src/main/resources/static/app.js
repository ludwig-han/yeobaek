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
