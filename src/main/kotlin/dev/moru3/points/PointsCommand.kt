package dev.moru3.points

import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.permissions.PermissionDefault
import org.bukkit.scoreboard.Team
import java.math.BigInteger
import java.util.Date
import java.util.UUID

class PointsCommand(main: Points): CommandExecutor, TabCompleter {
    // ポイントの追加や管理などの管理コマンドを実行するために必要な権限です。
    private val adminCommandPermission = "points.admin"
    // points.ymlファイルです。プレイヤーのポイントの情報が格納されています。
    private var config: FileConfiguration = main.config.config()?:throw IllegalStateException("failed to load 'plugin.yml'")
    // languages.ymlファイルです。プラグインのメッセージなどの言語関連が格納されています。
    private var languages: FileConfiguration = main.languagesConfig.config()?:throw IllegalStateException("failed to load 'plugin.yml'")
    // 設定できるポイントの最低値です。
    private val minSettablePoint = BigInteger.ZERO
    // サーバー起動中に参加したすべてのプレイヤーが格納されています。
    val players = mutableMapOf<String,OfflinePlayer>()
    // サーバーのメインスコアボードを保存。
    val mainScoreboard = Bukkit.getScoreboardManager()?.mainScoreboard?:throw IllegalStateException("failed to get scoreboard")

    init {
        // adminCommandPermission権限をサーバーに登録します。デフォルトはOPです。
        Bukkit.getServer().pluginManager.getPermission(adminCommandPermission)?.apply {
            default = PermissionDefault.OP
            setDescription("プレイヤーのポイントを設定、変更、表示する際などに必要な権限です。")
        }
        // プレイヤーがサーバーに参加した際にplayersに情報を保存。
        main.registerEvent<PlayerJoinEvent> { players[this.player.name] = this.player }
    }

    /**
     * pointsコマンドの処理。
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // IllegalCommandExceptionがthrowされた際にtranslationKeyをもとにエラーを送信してreturnする。
        try {
            // 引数が指定されているかつ、プレイヤーが必要な権限を持っている場合はポイントの追加や削除、表示などの処理の実行を可能にし、ない場合はどんなargumentが存在する場合でも自分のポイントを表示するようにしています。
            if(sender.hasPermission(adminCommandPermission)&&args.isNotEmpty()) {
                when(args.getOrNull(0)) {
                    // ポイントを追加、削除するコマンド。
                    "add", "sub" -> {
                        // args1にポイントが数字で指定されている場合はbigintegerに変換、指定されていない、もしくは正常に変換できなかった場合はエラーを表示。
                        val point = args.getOrNull(1)?.toBigInteger()?:throw IllegalCommandException("command.error.need_specification_point")
                        // ポイント数が0以下の場合はエラーを表示。
                        if(point <= minSettablePoint) { throw IllegalArgumentException("command.error.point_below_min") }
                        // セレクターが指定されていない場合はエラーを表示。
                        if(args.size < 2) { throw IllegalCommandException("command.error.need_specification_selector") }
                        // セレクターを解析。(args1はポイントなのでargs2以上から)
                        val parsedSelector = parseSelector(args.toList().subList(2,args.size))
                        // args0がaddの場合はポイントを追加、それ以外(sub)の場合は削除。
                        if(args[0]=="add") {
                            // 該当するプレイヤー全員にポイントを追加。
                            parsedSelector.forEach { it.addPoint(point) }
                            // 結果を表示。
                            sender.sendMessage(languages.getTranslationMessage("command.add.success",point,parsedSelector.map { it.name }.joinToString(",")))
                        } else {
                            // 該当するプレイヤー全員からポイントを削除。
                            parsedSelector.forEach { it.subPoint(point) }
                            // 結果を表示。
                            sender.sendMessage(languages.getTranslationMessage("command.sub.success",point,parsedSelector.map { it.name }.joinToString(",")))
                        }

                    }
                    // 結果発表！！！！！！！！！！
                    "broadcast" -> {

                    }
                    // セレクターのテストをするためのコマンドです。
                    "test" -> {
                        // セレクターを解析し、該当するプレイヤー一覧を表示する。
                        sender.sendMessage(languages.getTranslationMessage("command.test.result", parseSelector(args.toList().subList(1,args.size)).map { it.name }.joinToString(" ")))
                    }
                    // ヘルプを出します。(だしません)
                    "help" -> {
                        // へるぷなんてないよ^^
                        sender.sendMessage("へるぷなんてないよ^^\nひんと: セレクターは左から順番に処理されます。") // TODO ヘルプを作成する
                    }
                    else -> throw IllegalCommandException("command.error.illegal_arguments")
                }
            } else {
                //senderがプレイヤーじゃない場合はエラーを送信して処理を終了する。
                check(sender is Player) { throw IllegalCommandException("command.error.illegal_sender") }

                // >>> メッセージを表示 >>>
                sender.sendMessage(languages.getTranslationMessage("point.history.header"))
                try {
                    sender.getPointHistory().joinToString("\n") { "${languages.getTranslationMessage("point.history.color_prefix")}${it.first}: ${it.second}" }
                } catch(e: Exception) {
                    sender.sendMessage(languages.getTranslationMessage("point.history.history_not_found"))
                }
                sender.sendMessage(languages.getTranslationMessage("point.history.separator"))
                sender.sendMessage(languages.getTranslationMessage("points.history.total",sender.getTotalPoint()))
                // <<< メッセージを表示 <<<
            }
        } catch(e: IllegalCommandException) {
            // translationKeyを元にエラーを出力し、該当するキーが存在しない場合は初期値を送信する。
            sender.sendMessage("${ChatColor.RED}${languages.getTranslationMessage(e.translationKey,e.values)}")
        } catch(e: Exception) {
            e.printStackTrace()
            // 想定外のエラーが発生したことを実行者に報告する。
            sender.sendMessage("${ChatColor.RED}${languages.getTranslationMessage("command.error.unexpected_error")}")
        }
        return true
    }

    // TODO add comment to function below
    fun selectorTabComplete(args: List<String>): MutableList<String> {
        val keyCategories = mutableListOf("players","teams","teams-filter","tags","tags-filter")
        val last = args.lastOrNull()?:return keyCategories
        val key: String
        val value: String?
        last.split(":").also {
            key = it.first()
            value = it.getOrNull(1)
        }
        if(value!=null) {
            if(keyCategories.contains(key)) {
                val values = value.split(",")
                val lastValue = values.last()
                val names = mutableListOf("all").also { list ->
                    when(key) {
                        "players" -> {
                            list.addAll(this.players.values.mapNotNull { it.name })
                        }
                        "teams","teams-filter" -> {
                            list.addAll(mainScoreboard.teams.map { it.name })
                        }
                        "tags","tags-filter" -> {
                            list.addAll(Bukkit.getWorlds().asSequence().map { it.entities }.flatten().map { it.scoreboardTags }.flatten().toSet())
                        }
                    }
                }
                return if(lastValue.isEmpty()) {
                    mutableListOf("!").also { it.addAll(names) }
                } else {
                    when {
                        names.any { lastValue.endsWith(it) } -> mutableListOf(",")
                        lastValue.startsWith("!") -> names.map { "!$it" }.filter { it.startsWith(lastValue) }.toMutableList()
                        else -> names.filter { it.startsWith(lastValue) }.toMutableList()
                    }
                }
            } else {
                return keyCategories.filter { it.startsWith(last) }.toMutableList()
            }
        } else {
            return keyCategories.filter { it.startsWith(last) }.toMutableList()
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): MutableList<String> {
        val oneDArgs = mutableSetOf("add","sub","broadcast","test","help")
        when(args.size) {
            1 -> {
                return oneDArgs.filter { it.startsWith(args[0]) }.toMutableList()
            }
            2 -> {
                when(args[0]) {
                    "add", "sub" -> {
                        return (0..9).map { it.toString() }.toMutableList().also { if(args[0].isEmpty()) { it.remove("0") } }
                    }
                    "test" -> { return selectorTabComplete(args.toList().subList(1,args.size)) }
                }
            }
            else -> { return selectorTabComplete(args.toList().subList(2,args.size)) }
        }
        return mutableListOf()
    }


    /**
     * プレイヤーのポイント履歴を取得します。ポイントの変化量と変更された日時を返します。
     * @receiver player 対象のプレイヤー。
     * @return 変更日時とポイントの変化量(相対値)です。
     */
    private fun OfflinePlayer.getPointHistory(): List<Pair<Date,BigInteger>> {
        return config.getConfigurationSection("${this.uniqueId}.history")?.getKeys(false)?.map { Date(it.toLongOrNull()?:throw IllegalStateException("found invalid value in player's point history")) to (config.getString("${this.uniqueId}.history.${it}")?.toBigIntegerOrNull()?:BigInteger.ZERO) }?:throw IllegalArgumentException("player's point history not found")
    }

    /**
     * プレイヤーの合計ポイントを取得します。
     * @receiver 対象のプレイヤー。
     * @return プレイヤーのポイントの合計値。
     */
    private fun OfflinePlayer.getTotalPoint(): BigInteger = config.getString("${this.uniqueId}.total")?.toBigInteger()?:BigInteger.ZERO

    /**
     * Configからメッセージを取得し、自動でカラーコード変換、置換文字列を置き換えます。
     * @param translationKey Configのメッセージのキー。
     * @param values 置換する値。%index を置換します。 example: message.replace("%0",values[0])
     * @return カラーコード変換、文字列置換が完了したConfigのメッセージ。
     */
    private fun FileConfiguration.getTranslationMessage(translationKey: String,vararg values: Any): String {
        var result = ChatColor.translateAlternateColorCodes('&',this.getString(translationKey))?:"${ChatColor.RED}not found message that match this translation key: $translationKey"
        values.forEachIndexed { index, s -> result = result.replace("%${index}",s.toString()) }
        return result
    }

    /**
     * セレクターを解析して対象のプレイヤーをリストで返します。
     *
     * 例: ["player:all","team:!oni"] # team、oniに所属していない全てのプレイヤーが対象。
     *
     * @param selector セレクターのコマンド引数。
     * @return 対象のプレイヤー。
     */
    private fun parseSelector(selector: List<String>): Set<OfflinePlayer> {
        // 結果用のリストを作成。
        val result = mutableSetOf<OfflinePlayer>()
        // セレクターを解析するためにループ。 :で分割してキーバリューに分ける。
        selector.associate { it.split(":").let { k1 -> k1.getOrNull(0) to k1.getOrNull(1) } }
            .forEach { (key, value) ->
                // keyで何のフィルターかを判別
                when(key) {
                    "players" -> {
                        // playersの場合は複数人している可能性がるので,で分割してから!(除外)で始まっている場合はinversion(反転)をtrueにしてforループ。valueがない場合はエラー。
                        value?.split(",")?.map { it.startsWith("!") to it.removePrefix("!") }?.forEach { (inversion, player) ->
                            // playerセレクターがallの場合はすべてのプレイヤーを追加、プレイヤー名の場合はthis.playersを参照し、存在しない場合はエラーを出す。
                            if(player=="all") {
                                // inversionがtrueの場合は候補から全てのプレイヤーを削除、falseの場合はすべてのプレイヤーを追加。
                                if(inversion) { result.removeAll(this.players.values.toSet()) } else { result.addAll(this.players.values) }
                            } else {
                                // playerが存在する場合はOfflinePlayerに、存在ない場合はエラーを出す。
                                val offlinePlayer = this.players[player]?:throw IllegalCommandException("command.error.player_not_found", player)
                                // inversionがtrueの場合はプレイヤーを候補から削除、falseの場合は追加。
                                if(inversion) { result.remove(offlinePlayer) } else { result.add(offlinePlayer) }
                            }
                        }?:throw IllegalCommandException("command.error.illegal_selector","${key}:${value?:""}")
                    }
                    "teams","teams-filter" -> {
                        val teams = mutableSetOf<Team>()
                        // teamsの場合は複数指定している可能性があるので,で分割してから!(除外)で始まっている場合はinversion(反転)をtrueにしてforループ。valueがない場合はエラー。
                        value?.split(",")?.map { it.startsWith("!") to it.removePrefix("!") }?.forEach { (inversion, team) ->
                            // playerセレクターがallの場合はすべてのプレイヤーを追加、プレイヤー名の場合はthis.playersを参照し、存在しない場合はエラーを出す。
                            if(team=="all") {
                                // inversionがtrueの場合は候補から全てのチームを削除、falseの場合は候補にすべてのチームを追加。
                                if(inversion) { teams.removeAll(mainScoreboard.teams) } else { teams.addAll(mainScoreboard.teams) }
                            } else {
                                // teamが存在する場合はTeam型に変換、存在しない場合はエラーを出す。
                                val teamObject = mainScoreboard.getTeam(team)?:throw IllegalCommandException("command.error.team_not_found", team)
                                // inversionがtrueの場合は候補からチームを削除、falseの場合は候補にチームを追加。
                                if(inversion) { teams.remove(teamObject) } else { teams.add(teamObject) }
                            }
                        }?:throw IllegalCommandException("command.error.illegal_selector","${key}:${value?:""}")
                        // keyがteamsかteams-filterかを判別
                        if(key=="teams") {
                            // teamsの場合は対象のチームに所属してるべてのプレイヤーをresultに追加
                            teams.forEach { team -> result.addAll(team.entries.mapNotNull { this.players[it] }) }
                        } else {
                            // teams-filterの場合は対象のチームに所属していないプレイヤーをresultから削除
                            result.removeAll { !teams.contains(mainScoreboard.getEntryTeam(it.name?:UUID.randomUUID().toString())) }
                        }
                    }
                    "tags","tags-filter" -> {
                        // 対象のタグ一覧
                        val tags = mutableSetOf<String>()
                        // 非効率的なことをしている理由は、左から右に処理するという流れを遵守するためです。
                        value?.split(",")?.forEach { tag ->
                            // タグをフォーマット(最初の!を消す)をする
                            val formattedTag = tag.removePrefix("!")
                            val inversion = tag.startsWith("!")
                            // フォーマットされたタグがallの場合、inversionの場合、それ以外で分ける
                            if(formattedTag=="all") {
                                // inversionがtrueの場合はtagsをすべて削除、falseの場合はワールド内にいるすべてのエンティティのタグをtagsに追加。
                                if(inversion) { tags.clear() } else { tags.addAll(Bukkit.getWorlds().asSequence().map { it.entities }.flatten().map { it.scoreboardTags }.flatten().toSet()) }
                            } else if(inversion) {
                                // inversionがtrueの場合はtagsからtagを削除
                                tags.remove(formattedTag)
                            } else {
                                // tagsにtagを追加。
                                tags.add(formattedTag)
                            }
                        }?:throw IllegalCommandException("command.error.illegal_selector","${key}:${value?:""}")
                        // keyがtagsかtags-filterかを判別
                        if(key=="tags") {
                            // 対象のタグが付属しているすべてのプレイヤーをresultに追加。
                            result.addAll(this.players.values.mapNotNull { it.player }.filter { it.scoreboardTags.any { tag -> tags.contains(tag) } })
                        } else {
                            // tags-filterの場合は対象のタグが付属していないプレイヤーをresultから削除
                            result.removeAll { it.player?.scoreboardTags?.any { tag -> tags.contains(tag) } != true }
                        }
                    }
                    else -> throw IllegalCommandException("command.error.illegal_selector","${key?:""}:${value?:""}")
                }
            }
        return result
    }

    /**
     * プレイヤーにポイントを追加します。
     * @receiver ポイントを追加するプレヤー。
     * @param value 何ポイント追加するかをBigIntegerで。
     * @return 追加したあとのプレイヤーの合計ポイント数。
     */
    fun OfflinePlayer.addPoint(value: BigInteger): BigInteger {
        val total = maxOf((config.getString("${this.uniqueId}.total")?.toBigIntegerOrNull()?: BigInteger.ZERO) + value, BigInteger.ZERO)
        config.set("${this.uniqueId}.history.${Date().time}",value)
        config.set("${this.uniqueId}.total",total)
        return total
    }

    /**
     * プレイヤーからポイントを削除します。
     * @receiver ポイントを削除するプレヤー。
     * @param value 何ポイント削除するかをBigIntegerで。
     * @return 削除したあとのプレイヤーの合計ポイント数。
     */
    fun OfflinePlayer.subPoint(value: BigInteger): BigInteger = this.addPoint(-value)
}