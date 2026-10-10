package org.videolan.vlc.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import org.videolan.vlc.compose.icons.MaterialIcon
import org.videolan.vlc.compose.icons.MaterialSymbols

/**
 * Category glyphs for settings sections. They reuse VLC's own preference icons (the same artwork
 * the Android preference screens used), so a contributor can pick one without shipping platform
 * drawables into the common model.
 */
object SettingsIcons {
    val Interface: MaterialIcon by lazy { icon("interface", 960f, INTERFACE) }
    val Video: MaterialIcon by lazy { icon("video", 24f, VIDEO) }
    val Subtitles: MaterialIcon by lazy { icon("subtitles", 24f, SUBTITLES) }
    val Audio: MaterialIcon by lazy { icon("audio", 48f, AUDIO) }
    val Equalizer: MaterialIcon by lazy { icon("equalizer", 960f, EQUALIZER) }
    val Casting: MaterialIcon by lazy { icon("casting", 48f, CASTING) }
    val ParentalControl: MaterialIcon by lazy { icon("parental", 24f, PARENTAL) }
    val RemoteAccess: MaterialIcon by lazy { icon("remote", 48f, REMOTE) }
    val AndroidAuto: MaterialIcon by lazy { icon("auto", 960f, AUTO) }
    val Advanced: MaterialIcon by lazy { icon("advanced", 960f, ADVANCED) }
    val OptionalFeatures: MaterialIcon get() = MaterialSymbols.Filled.Extension

    private fun icon(name: String, viewport: Float, path: String): MaterialIcon = MaterialIcon(
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = viewport,
            viewportHeight = viewport,
        ).addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black)).build(),
    )

    private const val INTERFACE = "M480,880q-82,0 -155,-31.5t-127.5,-86Q143,708 111.5,635T80,480q0,-83 32.5,-156t88,-127Q256,143 330,111.5T488,80q80,0 151,27.5t124.5,76q53.5,48.5 85,115T880,442q0,115 -70,176.5T640,680h-74q-9,0 -12.5,5t-3.5,11q0,12 15,34.5t15,51.5q0,50 -27.5,74T480,880ZM480,480ZM260,520q26,0 43,-17t17,-43q0,-26 -17,-43t-43,-17q-26,0 -43,17t-17,43q0,26 17,43t43,17ZM380,360q26,0 43,-17t17,-43q0,-26 -17,-43t-43,-17q-26,0 -43,17t-17,43q0,26 17,43t43,17ZM580,360q26,0 43,-17t17,-43q0,-26 -17,-43t-43,-17q-26,0 -43,17t-17,43q0,26 17,43t43,17ZM700,520q26,0 43,-17t17,-43q0,-26 -17,-43t-43,-17q-26,0 -43,17t-17,43q0,26 17,43t43,17ZM480,800q9,0 14.5,-5t5.5,-13q0,-14 -15,-33t-15,-57q0,-42 29,-67t71,-25h70q66,0 113,-38.5T800,442q0,-121 -92.5,-201.5T488,160q-136,0 -232,93t-96,227q0,133 93.5,226.5T480,800Z"
    private const val VIDEO = "M4,6.47L5.76,10H20v8H4V6.47M22,4h-4l2,4h-3l-2,-4h-2l2,4h-3l-2,-4H8l2,4H7L5,4H4c-1.1,0 -1.99,0.9 -1.99,2L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V4z"
    private const val SUBTITLES = "M20,4L4,4c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2L22,6c0,-1.1 -0.9,-2 -2,-2zM20,18L4,18L4,6h16v12zM6,10h2v2L6,12zM6,14h8v2L6,16zM16,14h2v2h-2zM10,10h8v2h-8z"
    private const val AUDIO = "m28,6c-2.2,0 -4,1.8 -4,4v17.1c-1.88,-1.08 -4.2,-1.499 -6.66,-0.639 -2.68,0.96 -4.741,3.339 -5.221,6.139 -0.92,5.48 3.722,10.161 9.182,9.301C25.221,41.28 28,37.679 28,33.699L28,14h4c2.2,0 4,-1.8 4,-4L36,6ZM20,30c2.209,0 4,1.791 4,4 0,2.209 -1.791,4 -4,4 -2.209,0 -4,-1.791 -4,-4 0,-2.209 1.791,-4 4,-4z"
    private const val EQUALIZER = "M240,800q-17,0 -28.5,-11.5T200,760v-240h-40q-17,0 -28.5,-11.5T120,480q0,-17 11.5,-28.5T160,440h160q17,0 28.5,11.5T360,480q0,17 -11.5,28.5T320,520h-40v240q0,17 -11.5,28.5T240,800ZM240,360q-17,0 -28.5,-11.5T200,320v-120q0,-17 11.5,-28.5T240,160q17,0 28.5,11.5T280,200v120q0,17 -11.5,28.5T240,360ZM400,360q-17,0 -28.5,-11.5T360,320q0,-17 11.5,-28.5T400,280h40v-80q0,-17 11.5,-28.5T480,160q17,0 28.5,11.5T520,200v80h40q17,0 28.5,11.5T600,320q0,17 -11.5,28.5T560,360L400,360ZM480,800q-17,0 -28.5,-11.5T440,760v-280q0,-17 11.5,-28.5T480,440q17,0 28.5,11.5T520,480v280q0,17 -11.5,28.5T480,800ZM720,800q-17,0 -28.5,-11.5T680,760v-80h-40q-17,0 -28.5,-11.5T600,640q0,-17 11.5,-28.5T640,600h160q17,0 28.5,11.5T840,640q0,17 -11.5,28.5T800,680h-40v80q0,17 -11.5,28.5T720,800ZM720,520q-17,0 -28.5,-11.5T680,480v-280q0,-17 11.5,-28.5T720,160q17,0 28.5,11.5T760,200v280q0,17 -11.5,28.5T720,520Z"
    private const val CASTING = "m38,12c1.1,0 2,0.9 2,2v20c0,1.1 -0.9,2 -2,2h-8c-1.1,0 -2,0.9 -2,2 0,1.1 0.9,2 2,2h10c2.2,0 4,-1.8 4,-4v-24c0,-2.2 -1.8,-4 -4,-4L8,8c-2.2,0 -4,1.8 -4,4v2c0,1.1 0.9,2 2,2 1.1,0 2,-0.9 2,-2v-2zM6.28,26.18c-1.2,-0.2 -2.28,0.78 -2.28,2 0,0.98 0.72,1.8 1.7,1.96 4.16,0.72 7.44,4 8.16,8.16 0.16,0.98 0.98,1.7 1.96,1.7 1.22,0 2.18,-1.08 2,-2.28 -0.96,-5.920 -5.64,-10.58 -11.54,-11.54zM4,34v6h6c0,-3.32 -2.68,-6 -6,-6zM6.2,18.1c-1.18,-0.1 -2.2,0.82 -2.2,2 0,1.02 0.76,1.88 1.76,1.98 8.54,0.82 15.34,7.62 16.16,16.16 0.1,1 0.96,1.74 1.98,1.74 1.2,0 2.12,-1.04 2,-2.22 -1.06,-10.38 -9.32,-18.62 -19.7,-19.66z"
    private const val PARENTAL = "M12,2L4,5v6.09c0,5.05 3.41,9.76 8,10.91c4.59,-1.15 8,-5.86 8,-10.91V5L12,2zM18,11.09c0,4 -2.550,7.7 -6,8.83c-3.45,-1.13 -6,-4.82 -6,-8.830V6.31l6,-2.12l6,2.12V11.09zM8.82,10.59L7.4,12l3.54,3.54l5.66,-5.66l-1.41,-1.41l-4.24,4.24L8.82,10.59z"
    private const val REMOTE = "M24,6C22.704,6 21.277,6.46 20.27,7.445C19.262,8.431 18.653,9.719 18.104,11.367L10.559,34L9,34C6.815,34 5,35.815 5,38L5,40C5,41.105 5.895,42 7,42L18,42C19.105,42 20,41.105 20,40C20,38.895 19.105,38 18,38L13.441,38L21.896,12.633C22.347,11.281 22.796,10.569 23.066,10.305C23.337,10.04 23.411,10 24,10C24.589,10 24.663,10.04 24.934,10.305C25.204,10.569 25.653,11.281 26.104,12.633L28.463,19.713C28.813,20.76 29.945,21.326 30.992,20.977C32.039,20.627 32.605,19.495 32.256,18.447L29.896,11.367C29.347,9.719 28.738,8.431 27.73,7.445C26.723,6.46 25.296,6 24,6zM42,22C32.083,22 24,30.083 24,40A2,2 0,0 0,26 42A2,2 0,0 0,28 40C28,32.244 34.244,26 42,26A2,2 0,0 0,44 24A2,2 0,0 0,42 22zM42,30C36.501,30 32,34.501 32,40A2,2 0,0 0,34 42A2,2 0,0 0,36 40C36,36.663 38.663,34 42,34A2,2 0,0 0,44 32A2,2 0,0 0,42 30zM42,38A2,2 0,0 0,40 40A2,2 0,0 0,42 42A2,2 0,0 0,44 40A2,2 0,0 0,42 38z"
    private const val AUTO = "M240,760v20q0,25 -17.5,42.5T180,840q-25,0 -42.5,-17.5T120,780v-286q0,-7 1,-14t3,-13l75,-213q8,-24 29,-39t47,-15h410q26,0 47,15t29,39l75,213q2,6 3,13t1,14v286q0,25 -17.5,42.5T780,840q-25,0 -42.5,-17.5T720,780v-20L240,760ZM232,400h496l-42,-120L274,280l-42,120ZM200,480v200,-200ZM300,640q25,0 42.5,-17.5T360,580q0,-25 -17.5,-42.5T300,520q-25,0 -42.5,17.5T240,580q0,25 17.5,42.5T300,640ZM660,640q25,0 42.5,-17.5T720,580q0,-25 -17.5,-42.5T660,520q-25,0 -42.5,17.5T600,580q0,25 17.5,42.5T660,640ZM200,680h560v-200L200,480v200Z"
    private const val ADVANCED = "M360,600q-100,0 -170,-70t-70,-170q0,-20 3,-40t11,-38q5,-10 12.5,-15t16.5,-7q9,-2 18.5,0.5T199,271l105,105 72,-72 -105,-105q-8,-8 -10.5,-17.5T260,163q2,-9 7,-16.5t15,-12.5q18,-8 38,-11t40,-3q100,0 170,70t70,170q0,23 -4,43.5T584,444l202,200q29,29 29,71t-29,71q-29,29 -71,29t-71,-30L444,584q-20,8 -40.5,12t-43.5,4ZM360,520q26,0 52,-8t47,-25l243,243q5,5 13.5,4.5T729,729q5,-5 5,-13.5t-5,-13.5L486,460q18,-20 26,-46.5t8,-53.5q0,-60 -38.5,-104.5T386,202l74,74q12,12 12,28t-12,28L332,460q-12,12 -28,12t-28,-12l-74,-74q9,57 53.5,95.5T360,520ZM469,469Z"
}
