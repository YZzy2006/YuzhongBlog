// Safety net: remove loading screen if Vue fails to mount within 4s
setTimeout(function() {
  var el = document.getElementById('loading-screen');
  if (el) {
    el.classList.add('fade-out');
    setTimeout(function() { el.remove(); }, 700);
  }
  document.documentElement.classList.add('app-ready');
}, 4000);
