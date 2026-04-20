// ImagePreviewDialog.jsx - 파일 미리보기 모달
// 이미지: 줌인/줌아웃, 드래그 이동, 마우스 휠 확대/축소
// PDF: 브라우저 내장 PDF 뷰어(iframe)로 표시
// props:
// - file: 미리보기할 파일 객체 (image/* 또는 application/pdf)
// - onClose: 모달 닫기 함수
import { useState, useEffect, useRef, useCallback } from 'react'
import api from '../api/axios'

const MIN_ZOOM = 0.25
const MAX_ZOOM = 5
const ZOOM_STEP = 0.25

export default function ImagePreviewDialog({ file, onClose }) {
  // PDF인지 이미지인지 구분
  const isPdf = file.mimeType === 'application/pdf'

  const [viewUrl, setViewUrl] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  // 줌 레벨 (이미지 전용, PDF는 브라우저 내장 뷰어 사용)
  const [zoom, setZoom] = useState(1)
  // 드래그로 이미지 이동할 때 사용하는 좌표 (px)
  const [offset, setOffset] = useState({ x: 0, y: 0 })
  const [dragging, setDragging] = useState(false)
  const dragStart = useRef({ mouseX: 0, mouseY: 0, offsetX: 0, offsetY: 0 })

  // ref: 컨테이너(뷰포트)와 이미지 요소
  const containerRef = useRef(null)
  const imgRef = useRef(null)

  // 파일이 바뀔 때마다 미리보기 URL을 새로 받아옵니다
  useEffect(() => {
    let ignore = false
    const fetchViewUrl = async () => {
      setLoading(true)
      setError('')
      setZoom(1)
      setOffset({ x: 0, y: 0 })
      try {
        const res = await api.get(`/api/files/${file.id}/view`)
        if (!ignore) setViewUrl(res.data.data?.viewUrl || null)
      } catch (err) {
        if (!ignore) setError(err.response?.data?.message || '미리보기를 불러오지 못했습니다')
      } finally {
        if (!ignore) setLoading(false)
      }
    }
    fetchViewUrl()
    return () => { ignore = true }
  }, [file.id])

  // ──────────────────────────────────────
  // 오프셋 제한 함수
  // 확대된 이미지가 컨테이너 밖으로 나가지 않도록
  // 허용 가능한 최대 이동 거리를 계산해서 제한합니다
  // ──────────────────────────────────────
  const clampOffset = useCallback((newOffset, currentZoom) => {
    const container = containerRef.current
    const img = imgRef.current
    if (!container || !img) return newOffset

    // 이미지의 렌더링 크기 (CSS에 의해 축소된 실제 표시 크기)
    const imgW = img.offsetWidth
    const imgH = img.offsetHeight
    const containerW = container.clientWidth
    const containerH = container.clientHeight

    // 확대된 이미지 크기
    const scaledW = imgW * currentZoom
    const scaledH = imgH * currentZoom

    // 확대된 이미지가 컨테이너보다 클 때만 이동 허용
    // 이동 가능한 최대 거리 = (확대된 크기 - 컨테이너 크기) / 2
    const maxX = Math.max(0, (scaledW - containerW) / 2)
    const maxY = Math.max(0, (scaledH - containerH) / 2)

    return {
      x: Math.max(-maxX, Math.min(maxX, newOffset.x)),
      y: Math.max(-maxY, Math.min(maxY, newOffset.y)),
    }
  }, [])

  const clampZoom = (val) => Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, val))

  // ──────────────────────────────────────
  // 줌 변경 시 offset 보정
  // 줌아웃하면 offset을 비례 축소하고, zoom ≤ 1이면 센터링
  // ──────────────────────────────────────
  const applyZoom = useCallback((newZoom) => {
    setZoom((prevZoom) => {
      const clamped = clampZoom(newZoom)
      if (clamped <= 1) {
        // 100% 이하면 항상 가운데 정렬
        setOffset({ x: 0, y: 0 })
      } else {
        // 줌 비율에 맞게 offset을 스케일링한 뒤 범위 제한
        setOffset((prev) => {
          const ratio = clamped / prevZoom
          const scaled = { x: prev.x * ratio, y: prev.y * ratio }
          return clampOffset(scaled, clamped)
        })
      }
      return clamped
    })
  }, [clampOffset])

  const handleZoomIn = () => applyZoom(zoom + ZOOM_STEP)
  const handleZoomOut = () => applyZoom(zoom - ZOOM_STEP)
  const handleZoomReset = () => {
    setZoom(1)
    setOffset({ x: 0, y: 0 })
  }

  // 마우스 휠로 줌 조절
  const handleWheel = useCallback((e) => {
    e.preventDefault()
    const delta = e.deltaY > 0 ? -ZOOM_STEP : ZOOM_STEP
    setZoom((prev) => {
      const next = clampZoom(prev + delta)
      if (next <= 1) {
        setOffset({ x: 0, y: 0 })
      } else {
        setOffset((prevOffset) => {
          const ratio = next / prev
          const scaled = { x: prevOffset.x * ratio, y: prevOffset.y * ratio }
          return clampOffset(scaled, next)
        })
      }
      return next
    })
  }, [clampOffset])

  // 컨테이너에 wheel 이벤트 등록 (passive: false 필요)
  useEffect(() => {
    const el = containerRef.current
    if (!el) return
    el.addEventListener('wheel', handleWheel, { passive: false })
    return () => el.removeEventListener('wheel', handleWheel)
  }, [handleWheel])

  // 키보드 단축키
  useEffect(() => {
    const handleKey = (e) => {
      if (e.key === 'Escape') onClose()
      if (e.key === '+' || e.key === '=') handleZoomIn()
      if (e.key === '-') handleZoomOut()
      if (e.key === '0') handleZoomReset()
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [onClose, zoom])

  // ──────────────────────────────────────
  // 드래그 핸들러 (화면 밖 이탈 방지 적용)
  // ──────────────────────────────────────
  const handleMouseDown = (e) => {
    if (zoom <= 1) return
    e.preventDefault()
    setDragging(true)
    dragStart.current = {
      mouseX: e.clientX,
      mouseY: e.clientY,
      offsetX: offset.x,
      offsetY: offset.y,
    }
  }

  const handleMouseMove = (e) => {
    if (!dragging) return
    const dx = e.clientX - dragStart.current.mouseX
    const dy = e.clientY - dragStart.current.mouseY
    const raw = {
      x: dragStart.current.offsetX + dx,
      y: dragStart.current.offsetY + dy,
    }
    // 범위 제한 적용
    setOffset(clampOffset(raw, zoom))
  }

  const handleMouseUp = () => {
    setDragging(false)
  }

  // 다운로드 버튼
  const handleDownload = async () => {
    try {
      const res = await api.get(`/api/files/${file.id}/download`)
      const url = res.data.data?.downloadUrl
      if (url) window.open(url, '_blank')
    } catch {
      alert('다운로드에 실패했습니다')
    }
  }

  const zoomPercent = Math.round(zoom * 100)

  return (
    <div
      className="fixed inset-0 bg-black/80 z-50 flex flex-col"
      onClick={onClose}
      onMouseMove={!isPdf ? handleMouseMove : undefined}
      onMouseUp={!isPdf ? handleMouseUp : undefined}
      onMouseLeave={!isPdf ? handleMouseUp : undefined}
    >
      {/* 상단 바 */}
      <div
        className="flex items-center justify-between px-6 py-3 bg-black/60 text-white shrink-0"
        onClick={(e) => e.stopPropagation()}
      >
        <p className="text-sm truncate flex-1 mr-4">{file.name}</p>
        <div className="flex items-center gap-2">
          {/* 줌 컨트롤 — 이미지일 때만 표시 (PDF는 내장 뷰어가 줌 제공) */}
          {!isPdf && (
            <>
              <div className="flex items-center gap-1 bg-white/10 rounded-lg px-1">
                <button
                  onClick={handleZoomOut}
                  disabled={zoom <= MIN_ZOOM}
                  className="px-2 py-1 text-sm hover:bg-white/20 rounded disabled:opacity-30"
                  title="축소 (-)"
                >
                  −
                </button>
                <button
                  onClick={handleZoomReset}
                  className="px-2 py-1 text-xs hover:bg-white/20 rounded min-w-[3rem] text-center"
                  title="원본 크기 (0)"
                >
                  {zoomPercent}%
                </button>
                <button
                  onClick={handleZoomIn}
                  disabled={zoom >= MAX_ZOOM}
                  className="px-2 py-1 text-sm hover:bg-white/20 rounded disabled:opacity-30"
                  title="확대 (+)"
                >
                  +
                </button>
              </div>
              <div className="w-px h-5 bg-white/30" />
            </>
          )}
          <button
            onClick={handleDownload}
            className="px-3 py-1 rounded-lg text-xs bg-white/20 hover:bg-white/30"
            title="다운로드"
          >
            다운로드
          </button>
          <button
            onClick={onClose}
            className="text-xl leading-none hover:text-gray-300 px-1"
            title="닫기 (ESC)"
          >
            ✕
          </button>
        </div>
      </div>

      {/* 컨텐츠 영역 — PDF면 iframe, 이미지면 줌/드래그 */}
      {isPdf ? (
        <div className="flex-1 flex items-center justify-center p-4">
          {loading && <p className="text-white">불러오는 중...</p>}
          {error && <p className="text-red-300">{error}</p>}
          {!loading && !error && viewUrl && (
            <iframe
              src={viewUrl}
              title={file.name}
              className="w-full h-full rounded-lg bg-white"
              style={{ maxWidth: '900px' }}
              onClick={(e) => e.stopPropagation()}
            />
          )}
        </div>
      ) : (
        <div
          ref={containerRef}
          className="flex-1 overflow-hidden flex items-center justify-center"
          onMouseDown={handleMouseDown}
          style={{ cursor: zoom > 1 ? (dragging ? 'grabbing' : 'grab') : 'default' }}
        >
          {loading && <p className="text-white">불러오는 중...</p>}
          {error && <p className="text-red-300">{error}</p>}
          {!loading && !error && viewUrl && (
            <img
              ref={imgRef}
              src={viewUrl}
              alt={file.name}
              draggable={false}
              onClick={(e) => e.stopPropagation()}
              className="rounded-lg shadow-2xl select-none"
              style={{
                transform: `scale(${zoom}) translate(${offset.x / zoom}px, ${offset.y / zoom}px)`,
                transformOrigin: 'center center',
                maxWidth: '90vw',
                maxHeight: 'calc(100vh - 60px)',
                objectFit: 'contain',
                transition: dragging ? 'none' : 'transform 0.15s ease-out',
              }}
            />
          )}
        </div>
      )}
    </div>
  )
}
