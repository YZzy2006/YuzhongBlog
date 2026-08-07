import { readFileSync, mkdirSync, existsSync, statSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'
import sharp from 'sharp'

const __dirname = dirname(fileURLToPath(import.meta.url))
const petsDir = join(__dirname, '..', 'public', 'pets')
const thumbsDir = join(petsDir, 'thumbs')
const petsFile = join(__dirname, '..', 'src', 'config', 'pets.js')

// 从 pets.js 提取 sprite 路径
const content = readFileSync(petsFile, 'utf-8')
const sprites = [...content.matchAll(/sprite:\s*'([^']+)'/g)].map(m => m[1])

mkdirSync(thumbsDir, { recursive: true })

let created = 0
for (const spritePath of sprites) {
  const filename = spritePath.split('/').pop()
  const inputFile = join(petsDir, filename)
  const outputFile = join(thumbsDir, filename.replace(/\.\w+$/, '.webp'))

  if (!existsSync(inputFile)) continue
  if (existsSync(outputFile) && statSync(outputFile).mtimeMs >= statSync(inputFile).mtimeMs) continue

  const meta = await sharp(inputFile).metadata()
  const cellW = Math.round(meta.width / 8)
  const cellH = Math.round(meta.height / 9)

  await sharp(inputFile)
    .extract({ left: 0, top: 0, width: cellW, height: cellH })
    .resize(48, 48, { fit: 'cover' })
    .webp({ quality: 80 })
    .toFile(outputFile)

  created++
}

console.log(`Thumbs: ${created} created, ${sprites.length - created} cached, ${sprites.length} total`)
