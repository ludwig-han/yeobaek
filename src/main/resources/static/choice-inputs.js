'use strict';
// Enhance the existing bound fields: the server still receives the same values.
(() => {
  const form = document.querySelector('[data-plan-form]');
  if (!form) return;
  const field = name => form.elements.namedItem(name);
  const change = input => {
    input.dispatchEvent(new Event('input', {bubbles:true}));
    input.dispatchEvent(new Event('change', {bubbles:true}));
  };
  function choices(input, label, values, emptyLabel = '미정', onPick) {
    if (!input) return;
    const select = document.createElement('select');
    select.setAttribute('aria-label', label);
    input.setAttribute('aria-label', label + ' 직접 입력');
    select.dataset.choiceFor = input.name;
    select.add(new Option(emptyLabel, ''));
    values.forEach(([value, text]) => select.add(new Option(text, value)));
    select.add(new Option('직접 입력', '__custom__'));
    const required = input.required;
    select.required = required;
    input.required = false;
    input.before(select);
    const clear = input.parentElement.querySelector('[data-clear-time]');
    function sync() {
      const known = [...select.options].some(o => o.value === input.value);
      select.value = known ? input.value : '__custom__';
      input.hidden = select.value !== '__custom__';
      input.required = required && !input.hidden;
      if (clear) clear.hidden = input.hidden;
    }
    select.addEventListener('change', () => {
      input.hidden = select.value !== '__custom__';
      input.required = required && !input.hidden;
      if (clear) clear.hidden = input.hidden;
      if (!input.hidden) { input.focus(); return; }
      input.value = select.value;
      if (onPick) onPick(select.value);
      change(input);
    });
    // Do not switch away from custom while the user is typing.
    input.addEventListener('input', () => {
      if (input.hidden) sync();
    });
    input.addEventListener('time-cleared', sync);
    sync();
    return select;
  }
  choices(field('region'), '어디로 가나요?', ['서울','수원','인천','부산','제주','강릉','경주','전주','대전','대구','광주'].map(v=>[v,v]));
  for (let i = 0; i < 6; i++) {
    const kinds = {'카페':'FOOD','박물관':'PLACE_VISIT','미술관':'PLACE_VISIT','가챠샵':'SHOPPING','소품샵':'SHOPPING','산책':'ACTIVITY'};
    choices(field(`candidates[${i}].name`), `후보 ${i+1} 선택`, Object.keys(kinds).map(v=>[v,v]), '후보 없음', value=>{
      if (kinds[value]) field(`candidates[${i}].kind`).value = kinds[value];
    });
  }
  choices(field('constraints.maxWaitMinutes'), '기다릴 수 있는 시간', [['0','기다리지 않을래요'],['15','15분까지'],['30','30분까지'],['60','1시간까지']]);
  choices(field('constraints.budgetPerPerson'), '1인 하루 예산', [['30000','3만 원'],['50000','5만 원'],['100000','10만 원'],['150000','15만 원']]);
  choices(field('constraints.walkingNote'), '걷기에 대해 더 정할까요?', [
    ['한 번에 20분 이내로 걷고 싶어요','한 번에 20분 이내'],['하루 1만 보 이내로 걷고 싶어요','하루 1만 보 이내'],['중간중간 쉬고 싶어요','중간중간 쉬기']]);
  const times = [];
  for (let h=0;h<24;h++) for (const m of ['00','30']) {
    const t=String(h).padStart(2,'0')+':'+m; times.push([t,t]);
  }
  form.querySelectorAll('[data-time-input]').forEach(input => {
    const label = input.name.includes('returnBy') ? '귀가 시각 선택' :
      `${input.name.includes('[0]') ? '첫 번째' : '두 번째'} 목표 ${input.name.endsWith('fixedTime') ? '정해진' : input.name.endsWith('earliest') ? '기존 시작' : '기존 종료'} 시각 선택`;
    choices(input, label, times);
  });
  function collapseContents(section, summaryText, open) {
    const details=document.createElement('details');
    details.open=open;
    const summary=document.createElement('summary');summary.textContent=summaryText;
    details.append(summary);
    while(section.firstChild) details.append(section.firstChild);
    section.append(details);
    return details;
  }
  const hasValue = node => [...node.querySelectorAll('input:not([type="hidden"]),textarea,select:not([data-choice-for])')]
    .some(input => input.type==='checkbox' ? input.checked : input.value && input.value!=='UNKNOWN' && !(input.hasAttribute('data-participant-label') && input.value===input.dataset.defaultLabel));
  const second=form.querySelectorAll('[data-anchor-times]')[1];
  if(second) collapseContents(second,'중요한 것 하나 더 · 선택',hasValue(second));
  form.querySelectorAll('[data-optional-section]').forEach(section=>{
    const title=section.querySelector('h2').textContent;
    collapseContents(section,title,hasValue(section));
  });
  form.querySelectorAll('[data-anchor-times] > details').forEach(details=>{
    details.open=hasValue(details);
  });
  // Generate a title only for a new, blank title; never replace an existing title.
  const title=field('title');
  const autoTitle=document.createElement('input');autoTitle.type='checkbox';
  autoTitle.checked=!title.value;
  const autoLabel=document.createElement('label');autoLabel.className='check';
  autoLabel.append(autoTitle,document.createTextNode('제목 자동으로 만들기'));
  title.parentElement.before(autoLabel);
  function updateTitle(){
    title.parentElement.hidden=autoTitle.checked;
    if(autoTitle.checked) title.value=`${field('date').value || ''} ${field('region').value || ''} 나들이`.trim().slice(0,100);
  }
  autoTitle.addEventListener('change',updateTitle);
  field('date').addEventListener('change',updateTitle);
  field('region').addEventListener('input',updateTitle);
  updateTitle();
  // Reveal the exact field on both native validation errors and server re-renders.
  form.addEventListener('invalid', event=>{
    let parent=event.target.parentElement;
    while(parent && parent!==form){if(parent.tagName==='DETAILS')parent.open=true;parent=parent.parentElement;}
    if(event.target.hidden && event.target.name){
      const select=[...form.querySelectorAll('[data-choice-for]')].find(s=>s.dataset.choiceFor===event.target.name);
      if(select){select.value='__custom__';event.target.hidden=false;}
    }
  },true);
})();
