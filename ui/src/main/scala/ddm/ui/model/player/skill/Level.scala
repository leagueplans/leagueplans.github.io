package ddm.ui.model.player.skill

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder

import scala.math.Ordering.Implicits.infixOrderingOps

enum Level(val raw: Int, val bound: Exp) {
  case L1 extends Level(1, Exp(0))
  case L2 extends Level(2, Exp(83))
  case L3 extends Level(3, Exp(174))
  case L4 extends Level(4, Exp(276))
  case L5 extends Level(5, Exp(388))
  case L6 extends Level(6, Exp(512))
  case L7 extends Level(7, Exp(650))
  case L8 extends Level(8, Exp(801))
  case L9 extends Level(9, Exp(969))
  case L10 extends Level(10, Exp(1154))
  case L11 extends Level(11, Exp(1358))
  case L12 extends Level(12, Exp(1584))
  case L13 extends Level(13, Exp(1833))
  case L14 extends Level(14, Exp(2107))
  case L15 extends Level(15, Exp(2411))
  case L16 extends Level(16, Exp(2746))
  case L17 extends Level(17, Exp(3115))
  case L18 extends Level(18, Exp(3523))
  case L19 extends Level(19, Exp(3973))
  case L20 extends Level(20, Exp(4470))
  case L21 extends Level(21, Exp(5018))
  case L22 extends Level(22, Exp(5624))
  case L23 extends Level(23, Exp(6291))
  case L24 extends Level(24, Exp(7028))
  case L25 extends Level(25, Exp(7842))
  case L26 extends Level(26, Exp(8740))
  case L27 extends Level(27, Exp(9730))
  case L28 extends Level(28, Exp(10824))
  case L29 extends Level(29, Exp(12031))
  case L30 extends Level(30, Exp(13363))
  case L31 extends Level(31, Exp(14833))
  case L32 extends Level(32, Exp(16456))
  case L33 extends Level(33, Exp(18247))
  case L34 extends Level(34, Exp(20224))
  case L35 extends Level(35, Exp(22406))
  case L36 extends Level(36, Exp(24815))
  case L37 extends Level(37, Exp(27473))
  case L38 extends Level(38, Exp(30408))
  case L39 extends Level(39, Exp(33648))
  case L40 extends Level(40, Exp(37224))
  case L41 extends Level(41, Exp(41171))
  case L42 extends Level(42, Exp(45529))
  case L43 extends Level(43, Exp(50339))
  case L44 extends Level(44, Exp(55649))
  case L45 extends Level(45, Exp(61512))
  case L46 extends Level(46, Exp(67983))
  case L47 extends Level(47, Exp(75127))
  case L48 extends Level(48, Exp(83014))
  case L49 extends Level(49, Exp(91721))
  case L50 extends Level(50, Exp(101333))
  case L51 extends Level(51, Exp(111945))
  case L52 extends Level(52, Exp(123660))
  case L53 extends Level(53, Exp(136594))
  case L54 extends Level(54, Exp(150872))
  case L55 extends Level(55, Exp(166636))
  case L56 extends Level(56, Exp(184040))
  case L57 extends Level(57, Exp(203254))
  case L58 extends Level(58, Exp(224466))
  case L59 extends Level(59, Exp(247886))
  case L60 extends Level(60, Exp(273742))
  case L61 extends Level(61, Exp(302288))
  case L62 extends Level(62, Exp(333804))
  case L63 extends Level(63, Exp(368599))
  case L64 extends Level(64, Exp(407015))
  case L65 extends Level(65, Exp(449428))
  case L66 extends Level(66, Exp(496254))
  case L67 extends Level(67, Exp(547953))
  case L68 extends Level(68, Exp(605032))
  case L69 extends Level(69, Exp(668051))
  case L70 extends Level(70, Exp(737627))
  case L71 extends Level(71, Exp(814445))
  case L72 extends Level(72, Exp(899257))
  case L73 extends Level(73, Exp(992895))
  case L74 extends Level(74, Exp(1096278))
  case L75 extends Level(75, Exp(1210421))
  case L76 extends Level(76, Exp(1336443))
  case L77 extends Level(77, Exp(1475581))
  case L78 extends Level(78, Exp(1629200))
  case L79 extends Level(79, Exp(1798808))
  case L80 extends Level(80, Exp(1986068))
  case L81 extends Level(81, Exp(2192818))
  case L82 extends Level(82, Exp(2421087))
  case L83 extends Level(83, Exp(2673114))
  case L84 extends Level(84, Exp(2951373))
  case L85 extends Level(85, Exp(3258594))
  case L86 extends Level(86, Exp(3597792))
  case L87 extends Level(87, Exp(3972294))
  case L88 extends Level(88, Exp(4385776))
  case L89 extends Level(89, Exp(4842295))
  case L90 extends Level(90, Exp(5346332))
  case L91 extends Level(91, Exp(5902831))
  case L92 extends Level(92, Exp(6517253))
  case L93 extends Level(93, Exp(7195629))
  case L94 extends Level(94, Exp(7944614))
  case L95 extends Level(95, Exp(8771558))
  case L96 extends Level(96, Exp(9684577))
  case L97 extends Level(97, Exp(10692629))
  case L98 extends Level(98, Exp(11805606))
  case L99 extends Level(99, Exp(13034431))

  def next: Option[Level] =
    this match {
      case L99 => None
      case other => Some(Level(raw + 1))
    }

  override def toString: String =
    raw.toString
}

object Level {
  def apply(i: Int): Level = {
    val boundedLevel = Math.max(1, Math.min(i, 99))
    fromOrdinal(boundedLevel - 1)
  }

  def of(exp: Exp): Level =
    sortedDesc
      .dropWhile(_.bound > exp)
      .headOption
      .getOrElse(L1)

  private val sortedDesc: List[Level] =
    values.toList.sorted.reverse

  given Ordering[Level] = Ordering.by(_.raw)

  given Encoder[Level] = Encoder.derived
  given Decoder[Level] = Decoder.derived
}
