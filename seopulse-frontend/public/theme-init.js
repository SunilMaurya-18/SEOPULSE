// Applies the saved or preferred theme before first paint. Kept as a file
// (not inline) so the Content-Security-Policy can forbid inline scripts.
(function () {
  try {
    var t = localStorage.getItem('seopulse-theme')
    if (t !== 'light' && t !== 'dark') {
      t = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
    }
    document.documentElement.classList.add(t)
  } catch (e) {
    document.documentElement.classList.add('light')
  }
})()
