import { config } from 'md-editor-v3'
import hljs from 'highlight.js/lib/core'
import 'highlight.js/styles/atom-one-dark.min.css'

// Only register commonly used languages (not all 190+)
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import python from 'highlight.js/lib/languages/python'
import java from 'highlight.js/lib/languages/java'
import css from 'highlight.js/lib/languages/css'
import xml from 'highlight.js/lib/languages/xml'
import json from 'highlight.js/lib/languages/json'
import bash from 'highlight.js/lib/languages/bash'
import sql from 'highlight.js/lib/languages/sql'
import markdown from 'highlight.js/lib/languages/markdown'
import yaml from 'highlight.js/lib/languages/yaml'
import dockerfile from 'highlight.js/lib/languages/dockerfile'
import go from 'highlight.js/lib/languages/go'
import rust from 'highlight.js/lib/languages/rust'
import cpp from 'highlight.js/lib/languages/cpp'
import csharp from 'highlight.js/lib/languages/csharp'
import php from 'highlight.js/lib/languages/php'
import ruby from 'highlight.js/lib/languages/ruby'
import swift from 'highlight.js/lib/languages/swift'
import kotlin from 'highlight.js/lib/languages/kotlin'

hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('python', python)
hljs.registerLanguage('java', java)
hljs.registerLanguage('css', css)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('json', json)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('shell', bash)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('markdown', markdown)
hljs.registerLanguage('yaml', yaml)
hljs.registerLanguage('dockerfile', dockerfile)
hljs.registerLanguage('go', go)
hljs.registerLanguage('rust', rust)
hljs.registerLanguage('cpp', cpp)
hljs.registerLanguage('csharp', csharp)
hljs.registerLanguage('php', php)
hljs.registerLanguage('ruby', ruby)
hljs.registerLanguage('swift', swift)
hljs.registerLanguage('kotlin', kotlin)

// Lazy-load heavy extensions only when actually needed
let configured = false

export async function ensureMdConfig() {
  if (configured) return
  configured = true

  const [katexMod, mermaidMod, echartsMod] = await Promise.all([
    import('katex'),
    import('mermaid'),
    import('echarts')
  ])

  await import('katex/dist/katex.min.css')

  config({
    editorExtensions: {
      highlight: { instance: hljs },
      katex: { instance: katexMod.default },
      mermaid: { instance: mermaidMod.default },
      echarts: {
        instance: echartsMod,
        parseOption: (text) => JSON.parse(text)
      }
    },
    // Disable CDN loading to prevent Tracking Prevention warnings
    editorConfig: {
      languageUserDefined: {}
    }
  })
}

ensureMdConfig()
