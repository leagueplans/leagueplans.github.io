package ddm.ui.model.skill

import scala.math.Ordering.Implicits.infixOrderingOps

sealed abstract case class Level(raw: Int, bound: Exp) {
  lazy val next: Option[Level] =
    raw match {
      case 99 => None
      case _ => Some(Level(raw + 1))
    }

  override def toString: String =
    raw.toString
}

object Level {
  private val all: Map[Int, Level] =
    Map(
      1 -> Exp(0),
      2 -> Exp(83),
      3 -> Exp(174),
      4 -> Exp(276),
      5 -> Exp(388),
      6 -> Exp(512),
      7 -> Exp(650),
      8 -> Exp(801),
      9 -> Exp(969),
      10 -> Exp(1154),
      11 -> Exp(1358),
      12 -> Exp(1584),
      13 -> Exp(1833),
      14 -> Exp(2107),
      15 -> Exp(2411),
      16 -> Exp(2746),
      17 -> Exp(3115),
      18 -> Exp(3523),
      19 -> Exp(3973),
      20 -> Exp(4470),
      21 -> Exp(5018),
      22 -> Exp(5624),
      23 -> Exp(6291),
      24 -> Exp(7028),
      25 -> Exp(7842),
      26 -> Exp(8740),
      27 -> Exp(9730),
      28 -> Exp(10824),
      29 -> Exp(12031),
      30 -> Exp(13363),
      31 -> Exp(14833),
      32 -> Exp(16456),
      33 -> Exp(18247),
      34 -> Exp(20224),
      35 -> Exp(22406),
      36 -> Exp(24815),
      37 -> Exp(27473),
      38 -> Exp(30408),
      39 -> Exp(33648),
      40 -> Exp(37224),
      41 -> Exp(41171),
      42 -> Exp(45529),
      43 -> Exp(50339),
      44 -> Exp(55649),
      45 -> Exp(61512),
      46 -> Exp(67983),
      47 -> Exp(75127),
      48 -> Exp(83014),
      49 -> Exp(91721),
      50 -> Exp(101333),
      51 -> Exp(111945),
      52 -> Exp(123660),
      53 -> Exp(136594),
      54 -> Exp(150872),
      55 -> Exp(166636),
      56 -> Exp(184040),
      57 -> Exp(203254),
      58 -> Exp(224466),
      59 -> Exp(247886),
      60 -> Exp(273742),
      61 -> Exp(302288),
      62 -> Exp(333804),
      63 -> Exp(368599),
      64 -> Exp(407015),
      65 -> Exp(449428),
      66 -> Exp(496254),
      67 -> Exp(547953),
      68 -> Exp(605032),
      69 -> Exp(668051),
      70 -> Exp(737627),
      71 -> Exp(814445),
      72 -> Exp(899257),
      73 -> Exp(992895),
      74 -> Exp(1096278),
      75 -> Exp(1210421),
      76 -> Exp(1336443),
      77 -> Exp(1475581),
      78 -> Exp(1629200),
      79 -> Exp(1798808),
      80 -> Exp(1986068),
      81 -> Exp(2192818),
      82 -> Exp(2421087),
      83 -> Exp(2673114),
      84 -> Exp(2951373),
      85 -> Exp(3258594),
      86 -> Exp(3597792),
      87 -> Exp(3972294),
      88 -> Exp(4385776),
      89 -> Exp(4842295),
      90 -> Exp(5346332),
      91 -> Exp(5902831),
      92 -> Exp(6517253),
      93 -> Exp(7195629),
      94 -> Exp(7944614),
      95 -> Exp(8771558),
      96 -> Exp(9684577),
      97 -> Exp(10692629),
      98 -> Exp(11805606),
      99 -> Exp(13034431)
    ).map { case (i, exp) => i -> new Level(i, exp) {} }

  private val sortedDesc: List[Level] =
    all
      .values
      .toList
      .sortBy(_.bound)
      .reverse

  def apply(i: Int): Level =
    all(Math.max(
      Math.min(i, 99),
      1
    ))

  def of(exp: Exp): Level =
    sortedDesc
      .dropWhile(_.bound > exp)
      .headOption
      .getOrElse(Level(1))
}
