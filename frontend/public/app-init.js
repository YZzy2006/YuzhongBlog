// 如果同会话已看过加载动画，立即显示内容（跳过白屏）
if (sessionStorage.getItem('app-loaded') === 'true') {
  document.documentElement.classList.add('app-ready');
}
