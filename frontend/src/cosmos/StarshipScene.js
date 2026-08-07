import * as THREE from 'three'

export default class StarshipScene {
  constructor() {
    this.scene = new THREE.Scene()
    this.camera = new THREE.PerspectiveCamera(45, 1, 0.1, 100)
    this.camera.position.set(0, 1.5, 8)
    this.renderer = null
    this.clock = new THREE.Clock()
    this.ship = null
    this.pointsMesh = null
    this.exhaustMesh = null
    this.exhaustCoreMesh = null
    this.disposed = false
    this._animId = null
    this._starfieldMeshes = []
    this._onResize = null
    this._mouse = { x: 0, y: 0 }
    this._onMouseMove = this._onMouseMove.bind(this)
    this._loaded = false
  }

  async init(container) {
    this.renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    this.renderer.setSize(container.clientWidth, container.clientHeight)
    this.renderer.setClearColor(0x000000, 0)
    container.appendChild(this.renderer.domElement)

    const amb = new THREE.AmbientLight(0xffffff, 0.3)
    this.scene.add(amb)

    const glowYellow = new THREE.PointLight(0xeab308, 1.5, 20)
    glowYellow.position.set(2, 2, 3)
    this.scene.add(glowYellow)

    const glowBlue = new THREE.PointLight(0x3b82f6, 1, 20)
    glowBlue.position.set(-2, -1, 2)
    this.scene.add(glowBlue)

    this._addStarfield()

    this._onResize = () => {
      if (this.disposed) return
      const w = container.clientWidth
      const h = container.clientHeight
      this.camera.aspect = w / h
      this.camera.updateProjectionMatrix()
      this.renderer.setSize(w, h)
    }
    window.addEventListener('resize', this._onResize)
    window.addEventListener('mousemove', this._onMouseMove)

    this._buildTaichuShip()
    this._loaded = true
    this._animate()
  }

  _createShipMesh(geo, color = 0xeab308, size = 0.035) {
    const mat = new THREE.PointsMaterial({
      color,
      size,
      sizeAttenuation: true,
      transparent: true,
      opacity: 0.7,
      blending: THREE.AdditiveBlending,
      depthWrite: false
    })

    mat.onBeforeCompile = (shader) => {
      shader.uniforms.uTime = { value: 0 }
      mat.userData.shader = shader
      shader.vertexShader = 'attribute float aPhase;\nvarying float vPhase;\n' + shader.vertexShader
      shader.vertexShader = shader.vertexShader.replace('#include <begin_vertex>', '#include <begin_vertex>\nvPhase = aPhase;')
      shader.fragmentShader = 'uniform float uTime;\nvarying float vPhase;\n' + shader.fragmentShader
      shader.fragmentShader = shader.fragmentShader.replace(
        'vec4 diffuseColor = vec4( diffuse, opacity );',
        'float blink = (sin(uTime * 4.0 + vPhase) + 1.0) * 0.5; vec4 diffuseColor = vec4( diffuse, opacity * (blink * 0.7 + 0.3) );'
      )
    }

    return new THREE.Points(geo, mat)
  }

  _buildTaichuShip() {
    const count = 10000
    const positions = new Float32Array(count * 3)
    let idx = 0

    const addPoint = (x, y, z, jitter = 0.03) => {
      if (idx >= count) return
      positions[idx * 3] = x + (Math.random() - 0.5) * jitter
      positions[idx * 3 + 1] = y + (Math.random() - 0.5) * jitter
      positions[idx * 3 + 2] = z + (Math.random() - 0.5) * jitter
      idx++
    }

    // === MAIN HULL: tapered hexagonal prism (front-heavy diamond) ===
    for (let i = 0; i < count * 0.35; i++) {
      const t = Math.random() * 2 - 1
      const width = (1 - t * t) * 0.4
      const height = (1 - t * t) * 0.25
      const angle = Math.floor(Math.random() * 6) * (Math.PI / 3) + Math.random() * (Math.PI / 3)
      const r = Math.random()
      const hx = Math.cos(angle) * r * width
      const hy = Math.sin(angle) * r * height
      addPoint(t * 2.5, hy, hx, 0.02)
    }

    // === DORSAL SPINE: ridge along the top ===
    for (let i = 0; i < count * 0.08; i++) {
      const t = Math.random() * 2 - 1
      const spineHeight = (1 - t * t) * 0.35 + 0.15
      addPoint(t * 2.0, spineHeight, 0, 0.02)
    }

    // === SWEPT WINGS: delta shape, angled back ===
    for (let i = 0; i < count * 0.25; i++) {
      const side = Math.random() > 0.5 ? 1 : -1
      const span = Math.random() * 1.6
      const chord = (1 - span / 1.6) * 1.2
      const x = -span * 0.4 + (Math.random() - 0.3) * chord
      const y = -0.05 + Math.random() * 0.03
      const z = side * (0.3 + span * 0.5)
      addPoint(x, y, z, 0.02)
    }

    // === CANARD FOREPLANES: small forward fins ===
    for (let i = 0; i < count * 0.06; i++) {
      const side = Math.random() > 0.5 ? 1 : -1
      const span = Math.random() * 0.5
      const x = 1.5 + Math.random() * 0.4
      const y = 0.05 + Math.random() * 0.02
      const z = side * (0.2 + span * 0.4)
      addPoint(x, y, z, 0.015)
    }

    // === VENTRAL FIN: downward keel ===
    for (let i = 0; i < count * 0.05; i++) {
      const t = Math.random()
      const finHeight = (1 - t) * 0.4
      const x = -0.5 - t * 1.2
      addPoint(x, -0.15 - finHeight, 0, 0.02)
    }

    // === ENGINE NACELLES: two cylindrical pods at the rear ===
    for (let i = 0; i < count * 0.12; i++) {
      const side = Math.random() > 0.5 ? 1 : -1
      const angle = Math.random() * Math.PI * 2
      const r = Math.random() * 0.1
      const x = -1.8 + Math.random() * 0.6
      const y = Math.cos(angle) * r - 0.05
      const z = side * 0.5 + Math.sin(angle) * r
      addPoint(x, y, z, 0.015)
    }

    // === EXHAUST TRAIL: outer blue glow ===
    const exhaustCount = Math.floor(count * 0.07)
    const exhaustPositions = new Float32Array(exhaustCount * 3)
    let eidx = 0
    const addExhaust = (x, y, z) => {
      if (eidx >= exhaustCount) return
      exhaustPositions[eidx * 3] = x
      exhaustPositions[eidx * 3 + 1] = y
      exhaustPositions[eidx * 3 + 2] = z
      eidx++
    }
    for (let i = 0; i < exhaustCount; i++) {
      const side = Math.random() > 0.5 ? 1 : -1
      const spread = Math.random() * 0.1
      const depth = -2.2 - Math.random() * 1.8
      addExhaust(
        depth,
        -0.05 + (Math.random() - 0.5) * spread,
        side * 0.5 + (Math.random() - 0.5) * spread
      )
    }

    // === EXHAUST CORE: bright white-blue inner flame ===
    const coreCount = Math.floor(count * 0.02)
    const corePositions = new Float32Array(coreCount * 3)
    let cidx = 0
    for (let i = 0; i < coreCount; i++) {
      const side = Math.random() > 0.5 ? 1 : -1
      const depth = -2.2 - Math.random() * 1.0
      corePositions[i * 3] = depth
      corePositions[i * 3 + 1] = -0.05 + (Math.random() - 0.5) * 0.03
      corePositions[i * 3 + 2] = side * 0.5 + (Math.random() - 0.5) * 0.03
      cidx++
    }

    // Build main ship geometry
    const geo = new THREE.BufferGeometry()
    geo.setAttribute('position', new THREE.BufferAttribute(positions.slice(0, idx * 3), 3))
    geo.computeBoundingBox()
    const center = new THREE.Vector3()
    geo.boundingBox.getCenter(center)
    const posAttr = geo.attributes.position
    for (let i = 0; i < posAttr.count; i++) {
      posAttr.setXYZ(i, posAttr.getX(i) - center.x, posAttr.getY(i) - center.y, posAttr.getZ(i) - center.z)
    }
    posAttr.needsUpdate = true

    const phases = new Float32Array(posAttr.count)
    for (let i = 0; i < posAttr.count; i++) phases[i] = Math.random() * Math.PI * 2
    geo.setAttribute('aPhase', new THREE.BufferAttribute(phases, 1))

    // Build exhaust geometry
    const exhaustGeo = new THREE.BufferGeometry()
    exhaustGeo.setAttribute('position', new THREE.BufferAttribute(exhaustPositions.slice(0, eidx * 3), 3))
    const ePosAttr = exhaustGeo.attributes.position
    for (let i = 0; i < ePosAttr.count; i++) {
      ePosAttr.setXYZ(i, ePosAttr.getX(i) - center.x, ePosAttr.getY(i) - center.y, ePosAttr.getZ(i) - center.z)
    }
    ePosAttr.needsUpdate = true
    const exhaustPhases = new Float32Array(ePosAttr.count)
    for (let i = 0; i < ePosAttr.count; i++) exhaustPhases[i] = Math.random() * Math.PI * 2
    exhaustGeo.setAttribute('aPhase', new THREE.BufferAttribute(exhaustPhases, 1))

    // Build exhaust core geometry
    const coreGeo = new THREE.BufferGeometry()
    coreGeo.setAttribute('position', new THREE.BufferAttribute(corePositions.slice(0, cidx * 3), 3))
    const cPosAttr = coreGeo.attributes.position
    for (let i = 0; i < cPosAttr.count; i++) {
      cPosAttr.setXYZ(i, cPosAttr.getX(i) - center.x, cPosAttr.getY(i) - center.y, cPosAttr.getZ(i) - center.z)
    }
    cPosAttr.needsUpdate = true
    const corePhases = new Float32Array(cPosAttr.count)
    for (let i = 0; i < cPosAttr.count; i++) corePhases[i] = Math.random() * Math.PI * 2
    coreGeo.setAttribute('aPhase', new THREE.BufferAttribute(corePhases, 1))

    // Create meshes
    this.pointsMesh = this._createShipMesh(geo, 0xeab308, 0.035)
    this.exhaustMesh = this._createShipMesh(exhaustGeo, 0x3b82f6, 0.025)
    this.exhaustCoreMesh = this._createShipMesh(coreGeo, 0xbfdbfe, 0.015)

    this.ship = new THREE.Group()
    this.ship.add(this.pointsMesh)
    this.ship.add(this.exhaustMesh)
    this.ship.add(this.exhaustCoreMesh)
    this.ship.rotation.set(Math.PI / 0.9, -0.2, 3.0)
    this.ship.position.set(1.2, 0.8, 1)
    this.scene.add(this.ship)
  }

  _addStarfield() {
    // Base starfield
    const count = 500
    const geo = new THREE.BufferGeometry()
    const pos = new Float32Array(count * 3)
    for (let i = 0; i < count * 3; i++) pos[i] = (Math.random() - 0.5) * 40
    geo.setAttribute('position', new THREE.BufferAttribute(pos, 3))
    const mat = new THREE.PointsMaterial({ color: 0xffffff, size: 0.05, transparent: true, opacity: 0.4, depthWrite: false })
    const starMesh = new THREE.Points(geo, mat)
    this.scene.add(starMesh)
    this._starfieldMeshes.push(starMesh)

    // Subtle nebula particles for depth
    const nebulaColors = [0x1e3a5f, 0x4c1d95, 0x134e4a]
    for (const color of nebulaColors) {
      const n = 60
      const ng = new THREE.BufferGeometry()
      const np = new Float32Array(n * 3)
      for (let i = 0; i < n * 3; i++) np[i] = (Math.random() - 0.5) * 30
      ng.setAttribute('position', new THREE.BufferAttribute(np, 3))
      const nm = new THREE.PointsMaterial({ color, size: 0.15, transparent: true, opacity: 0.06, depthWrite: false, blending: THREE.AdditiveBlending })
      const nebMesh = new THREE.Points(ng, nm)
      this.scene.add(nebMesh)
      this._starfieldMeshes.push(nebMesh)
    }
  }

  _animate() {
    if (this.disposed) return
    this._animId = requestAnimationFrame(() => this._animate())

    const t = this.clock.getElapsedTime()
    if (this.ship) {
      this.ship.position.y = 0.8 + Math.sin(t * 0.5) * 0.15
      this.ship.position.x = 1.2 + Math.sin(t * 0.3) * 0.08
      this.ship.rotation.z = 3.0 + Math.sin(t * 0.4) * 0.02
      this.ship.rotation.x = Math.PI / 0.9 + Math.sin(t * 0.35) * 0.01
    }

    if (this.disposed) return

    // Update all shaders
    const shader1 = this.pointsMesh?.material?.userData?.shader
    if (shader1) shader1.uniforms.uTime.value = t
    const shader2 = this.exhaustMesh?.material?.userData?.shader
    if (shader2) shader2.uniforms.uTime.value = t
    const shader3 = this.exhaustCoreMesh?.material?.userData?.shader
    if (shader3) shader3.uniforms.uTime.value = t

    this.camera.position.x += (this._mouse.x * 0.5 - this.camera.position.x) * 0.02
    this.camera.position.y += (this._mouse.y * 0.3 + 1.5 - this.camera.position.y) * 0.02
    this.camera.lookAt(0, 0, 0)

    if (!this.disposed && this.renderer) {
      this.renderer.render(this.scene, this.camera)
    }
  }

  _onMouseMove(e) {
    this._mouse.x = (e.clientX / window.innerWidth) * 2 - 1
    this._mouse.y = -(e.clientY / window.innerHeight) * 2 + 1
  }

  getScreenPosition(worldPos) {
    if (!this.renderer) return { x: 0, y: 0 }
    const v = new THREE.Vector3(worldPos.x, worldPos.y, worldPos.z)
    if (this.ship) v.applyMatrix4(this.ship.matrixWorld)
    v.project(this.camera)
    const w = this.renderer.domElement.clientWidth
    const h = this.renderer.domElement.clientHeight
    return {
      x: (v.x * 0.5 + 0.5) * w,
      y: (-v.y * 0.5 + 0.5) * h
    }
  }

  dispose() {
    this.disposed = true
    if (this._animId) cancelAnimationFrame(this._animId)
    if (this._onResize) window.removeEventListener('resize', this._onResize)
    window.removeEventListener('mousemove', this._onMouseMove)
    if (this.pointsMesh) {
      this.pointsMesh.geometry.dispose()
      this.pointsMesh.material.dispose()
    }
    if (this.exhaustMesh) {
      this.exhaustMesh.geometry.dispose()
      this.exhaustMesh.material.dispose()
    }
    if (this.exhaustCoreMesh) {
      this.exhaustCoreMesh.geometry.dispose()
      this.exhaustCoreMesh.material.dispose()
    }
    for (const m of this._starfieldMeshes) {
      m.geometry.dispose()
      m.material.dispose()
    }
    this._starfieldMeshes = []
    if (this.renderer) {
      this.renderer.dispose()
      this.renderer.domElement.parentElement?.removeChild(this.renderer.domElement)
    }
  }
}
