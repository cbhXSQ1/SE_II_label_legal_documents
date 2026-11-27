// 数值与显示格式化

export function percent(v) {
  if (v == null || v === '' || Number.isNaN(Number(v))) return '—'
  return (Number(v)).toFixed(1) + '%'
}

export function fmtDateTime(s) {
  return s || ''
}

// 差异度 = 1 − 一致度；一致度 = 2 × 相同项数 / (甲项数 + 乙项数)
export function computeAgreement(aCount, bCount, sameCount) {
  if (aCount + bCount === 0) return { agreement: null, difference: null, note: '无数据' }
  const agreement = (2 * sameCount) / (aCount + bCount)
  return {
    agreement: agreement * 100,
    difference: (1 - agreement) * 100,
    note: ''
  }
}
