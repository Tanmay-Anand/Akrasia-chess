import { Chessboard } from 'react-chessboard'
import { Chess } from 'chess.js'

interface Props {
  fen?: string
  playerColor?: string
}

export function ChessBoard({ fen, playerColor = 'white' }: Props) {
  const safeFen = (() => {
    if (!fen) return undefined
    try {
      new Chess(fen)
      return fen
    } catch {
      return undefined
    }
  })()

  return (
    <Chessboard
      position={safeFen}
      boardOrientation={playerColor === 'black' ? 'black' : 'white'}
      arePiecesDraggable={false}
      customDarkSquareStyle={{ backgroundColor: '#3d5a80' }}
      customLightSquareStyle={{ backgroundColor: '#98c1d9' }}
      boardWidth={360}
    />
  )
}
