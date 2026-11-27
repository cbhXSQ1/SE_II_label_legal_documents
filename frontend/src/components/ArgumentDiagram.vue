<script setup>
import { ref, computed } from 'vue'

// diagrams: 数组，每个 { id, title, nodes:[{id,x,y}], relNodes:[{id,kind,x,y}], edges:[{from,to,arrow}] }
// 直接改父组件传入的响应式数组里的坐标（拖动即改 x/y）
const props = defineProps({
  diagrams: { type: Array, required: true }
})

const zoom = ref(1)
const W = 780
const H = 560

function pos(diagram, id) {
  const n = diagram.nodes.find((n) => n.id === id)
  if (n) return { x: n.x, y: n.y, type: 'node' }
  const r = diagram.relNodes.find((r) => r.id === id)
  if (r) return { x: r.x, y: r.y, type: 'rel' }
  return null
}

function findNode(diagram, id) {
  return diagram.nodes.find((n) => n.id === id) || diagram.relNodes.find((r) => r.id === id)
}

function nodeRadius(diagram, id) {
  return diagram.nodes.some((n) => n.id === id) ? 16 : 12
}

function arrowHeadPoints(tip, ux, uy) {
  const size = 8
  const px = -uy
  const py = ux
  const baseX = tip.x - ux * size
  const baseY = tip.y - uy * size
  return [
    `${tip.x},${tip.y}`,
    `${baseX + px * size * 0.55},${baseY + py * size * 0.55}`,
    `${baseX - px * size * 0.55},${baseY - py * size * 0.55}`
  ].join(' ')
}

function edgesOf(diagram) {
  return diagram.edges.map((e) => {
    const a = pos(diagram, e.from)
    const b = pos(diagram, e.to)
    if (!a || !b) return { ...e, start: null, end: null, head: null }
    const r = nodeRadius(diagram, e.to)
    const dx = b.x - a.x
    const dy = b.y - a.y
    const len = Math.hypot(dx, dy) || 1
    const ux = dx / len
    const uy = dy / len
    const start = { x: a.x + ux * r, y: a.y + uy * r }
    const end = { x: b.x - ux * r, y: b.y - uy * r }
    return { ...e, start, end, head: e.arrow ? arrowHeadPoints(end, ux, uy) : null }
  })
}

const rendered = computed(() => props.diagrams.map((dg) => ({ ...dg, edges: edgesOf(dg) })))

let dragging = null
function svgPoint(e, svg) {
  const rect = svg.getBoundingClientRect()
  const x = ((e.clientX - rect.left) / rect.width) * W
  const y = ((e.clientY - rect.top) / rect.height) * H
  return { x, y }
}
function onNodeDown(e, diagram, id) {
  dragging = { diagram, id, svg: e.currentTarget.ownerSVGElement }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}
function onMove(e) {
  if (!dragging) return
  const p = svgPoint(e, dragging.svg)
  const node = findNode(dragging.diagram, dragging.id)
  if (node) { node.x = Math.round(p.x); node.y = Math.round(p.y) }
}
function onUp() {
  dragging = null
  window.removeEventListener('mousemove', onMove)
  window.removeEventListener('mouseup', onUp)
}
</script>

<template>
  <div>
    <div class="toolbar">
      <span class="legend">
        <span class="lg"><i class="dot solid" />支持</span>
        <span class="lg"><i class="dot hollow" />反对</span>
        <span class="lg"><i class="dot plus">+</i>组合/匹配</span>
        <span class="lg"><i class="dot rect" />命题</span>
      </span>
      <el-button-group>
        <el-button size="small" @click="zoom = Math.max(0.5, +(zoom - 0.2).toFixed(1))">−</el-button>
        <el-button size="small" @click="zoom = 1">重置</el-button>
        <el-button size="small" @click="zoom = Math.min(2, +(zoom + 0.2).toFixed(1))">＋</el-button>
      </el-button-group>
    </div>

    <div v-if="!diagrams || diagrams.length === 0" class="empty">
      <el-empty description="暂无图示，请先在「关系」页建立关系后生成" />
    </div>

    <div v-for="dg in rendered" :key="dg.id" class="diagram-wrap">
      <div class="dg-title">{{ dg.title }}</div>
      <div class="svg-scroll">
        <svg :width="W * zoom" :height="H * zoom" viewBox="0 0 780 560" style="max-width: 100%; height: auto;">
          <!-- 边 -->
          <g v-for="e in dg.edges" :key="e.from + '-' + e.to">
            <line v-if="e.start" :x1="e.start.x" :y1="e.start.y" :x2="e.end.x" :y2="e.end.y" stroke="#909399" stroke-width="1.5" />
            <polygon v-if="e.head" :points="e.head" fill="#909399" />
          </g>
          <!-- 关系节点 -->
          <g v-for="r in dg.relNodes" :key="r.id" style="cursor: move;" @mousedown="onNodeDown($event, dg, r.id)">
            <circle v-if="r.kind === 'support'" :cx="r.x" :cy="r.y" r="12" fill="#303133" />
            <circle v-else-if="r.kind === 'attack'" :cx="r.x" :cy="r.y" r="12" fill="#fff" stroke="#303133" stroke-width="2" />
            <circle v-else :cx="r.x" :cy="r.y" r="12" fill="#fff" stroke="#409eff" stroke-width="2" />
            <text v-if="r.kind === 'plus'" :x="r.x" :y="r.y" text-anchor="middle" dominant-baseline="central" font-size="14" fill="#409eff">+</text>
          </g>
          <!-- 命题节点 -->
          <g v-for="n in dg.nodes" :key="n.id" style="cursor: move;" @mousedown="onNodeDown($event, dg, n.id)">
            <rect :x="n.x - 30" :y="n.y - 15" width="60" height="30" rx="4" fill="#ecf5ff" stroke="#409eff" stroke-width="1.5" />
            <text :x="n.x" :y="n.y" text-anchor="middle" dominant-baseline="central" font-size="13" fill="#303133">{{ n.id }}</text>
          </g>
        </svg>
      </div>
    </div>
  </div>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.legend { display: inline-flex; gap: 14px; align-items: center; }
.lg { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #606266; }
.dot { display: inline-block; width: 16px; height: 16px; border-radius: 50%; text-align: center; line-height: 16px; font-size: 12px; }
.dot.solid { background: #303133; }
.dot.hollow { background: #fff; border: 2px solid #303133; }
.dot.plus { background: #fff; border: 2px solid #409eff; color: #409eff; }
.dot.rect { background: #ecf5ff; border: 1.5px solid #409eff; border-radius: 3px; }
.diagram-wrap { background: #fff; border: 1px solid #e4e7ed; border-radius: 8px; padding: 12px; margin-bottom: 12px; }
.dg-title { font-weight: 600; margin-bottom: 8px; }
.svg-scroll { overflow: auto; max-height: 560px; border: 1px dashed #e4e7ed; border-radius: 4px; padding: 8px; }
.empty { padding: 20px 0; }
</style>
