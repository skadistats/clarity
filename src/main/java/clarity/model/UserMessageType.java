package clarity.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_AIDebugLine;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_AddQuestLogEntry;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_BotChat;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_ChatEvent;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_ChatWheel;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_ClientLoadGridNav;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_CoachHUDPing;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_CombatHeroPositions;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_CombatLogData;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_CombatLogShowDeath;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_CreateLinearProjectile;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_CustomMsg;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_DestroyLinearProjectile;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_DodgeTrackingProjectiles;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_GlobalLightColor;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_GlobalLightDirection;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_HalloweenDrops;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_HudError;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_InvalidCommand;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_ItemAlert;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_ItemFound;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_ItemPurchased;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_LocationPing;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_MapLine;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_MiniKillCamInfo;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_MinimapDebugPoint;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_MinimapEvent;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_NevermoreRequiem;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_OverheadEvent;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_ParticleManager;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_Ping;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_ReceivedXmasGift;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_SendFinalGold;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_SendGenericToolTip;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_SendRoshanPopup;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_SendStatPopup;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_SetNextAutobuyItem;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_SharedCooldown;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_ShowSurvey;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_SpectatorPlayerClick;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_SwapVerify;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_TournamentDrop;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_TutorialFade;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_TutorialFinish;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_TutorialPingMinimap;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_TutorialRequestExp;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_TutorialTipInfo;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_UnitEvent;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_UpdateSharedContent;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_WorldLine;
import com.dota2.proto.DotaUsermessages.CDOTA_UM_GamerulesStateChanged;
import com.dota2.proto.DotaUsermessages.EDotaUserMessages;
import com.dota2.proto.Usermessages.CUserMsg_AchievementEvent;
import com.dota2.proto.Usermessages.CUserMsg_CloseCaption;
import com.dota2.proto.Usermessages.CUserMsg_CurrentTimescale;
import com.dota2.proto.Usermessages.CUserMsg_DesiredTimescale;
import com.dota2.proto.Usermessages.CUserMsg_Fade;
import com.dota2.proto.Usermessages.CUserMsg_GameTitle;
import com.dota2.proto.Usermessages.CUserMsg_Geiger;
import com.dota2.proto.Usermessages.CUserMsg_HintText;
import com.dota2.proto.Usermessages.CUserMsg_HudMsg;
import com.dota2.proto.Usermessages.CUserMsg_HudText;
import com.dota2.proto.Usermessages.CUserMsg_KeyHintText;
import com.dota2.proto.Usermessages.CUserMsg_MessageText;
import com.dota2.proto.Usermessages.CUserMsg_RequestState;
import com.dota2.proto.Usermessages.CUserMsg_ResetHUD;
import com.dota2.proto.Usermessages.CUserMsg_Rumble;
import com.dota2.proto.Usermessages.CUserMsg_SayText;
import com.dota2.proto.Usermessages.CUserMsg_SayText2;
import com.dota2.proto.Usermessages.CUserMsg_SayTextChannel;
import com.dota2.proto.Usermessages.CUserMsg_SendAudio;
import com.dota2.proto.Usermessages.CUserMsg_Shake;
import com.dota2.proto.Usermessages.CUserMsg_ShakeDir;
import com.dota2.proto.Usermessages.CUserMsg_StatsCrawlMsg;
import com.dota2.proto.Usermessages.CUserMsg_StatsSkipState;
import com.dota2.proto.Usermessages.CUserMsg_TextMsg;
import com.dota2.proto.Usermessages.CUserMsg_Tilt;
import com.dota2.proto.Usermessages.CUserMsg_Train;
import com.dota2.proto.Usermessages.CUserMsg_VGUIMenu;
import com.dota2.proto.Usermessages.CUserMsg_VoiceMask;
import com.dota2.proto.Usermessages.CUserMsg_VoiceSubtitle;
import com.dota2.proto.Usermessages.EBaseUserMessages;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;

public enum UserMessageType {

    ACHIEVEMENT_EVENT(EBaseUserMessages.UM_AchievementEvent_VALUE, CUserMsg_AchievementEvent.class),
    CLOSE_CAPTION(EBaseUserMessages.UM_CloseCaption_VALUE, CUserMsg_CloseCaption.class),
    CLOSE_CAPTION_DIRECT(EBaseUserMessages.UM_CloseCaptionDirect_VALUE, CUserMsg_CloseCaption.class),
    CURRENT_TIMESCALE(EBaseUserMessages.UM_CurrentTimescale_VALUE, CUserMsg_CurrentTimescale.class),
    DESIRED_TIMESCALE(EBaseUserMessages.UM_DesiredTimescale_VALUE, CUserMsg_DesiredTimescale.class),
    FADE(EBaseUserMessages.UM_Fade_VALUE, CUserMsg_Fade.class),
    GAME_TITLE(EBaseUserMessages.UM_GameTitle_VALUE, CUserMsg_GameTitle.class),
    GEIGER(EBaseUserMessages.UM_Geiger_VALUE, CUserMsg_Geiger.class),
    HINT_TEXT(EBaseUserMessages.UM_HintText_VALUE, CUserMsg_HintText.class),
    HUD_MSG(EBaseUserMessages.UM_HudMsg_VALUE, CUserMsg_HudMsg.class),
    HUD_TEXT(EBaseUserMessages.UM_HudText_VALUE, CUserMsg_HudText.class),
    KEY_HINT_TEXT(EBaseUserMessages.UM_KeyHintText_VALUE, CUserMsg_KeyHintText.class),
    MESSAGE_TEXT(EBaseUserMessages.UM_MessageText_VALUE, CUserMsg_MessageText.class),
    REQUEST_STATE(EBaseUserMessages.UM_RequestState_VALUE, CUserMsg_RequestState.class),
    RESET_HUD(EBaseUserMessages.UM_ResetHUD_VALUE, CUserMsg_ResetHUD.class),
    RUMBLE(EBaseUserMessages.UM_Rumble_VALUE, CUserMsg_Rumble.class),
    SAY_TEXT(EBaseUserMessages.UM_SayText_VALUE, CUserMsg_SayText.class),
    SAY_TEXT_2(EBaseUserMessages.UM_SayText2_VALUE, CUserMsg_SayText2.class),
    SAY_TEXT_CHANNEL(EBaseUserMessages.UM_SayTextChannel_VALUE, CUserMsg_SayTextChannel.class),
    SHAKE(EBaseUserMessages.UM_Shake_VALUE, CUserMsg_Shake.class),
    SHAKE_DIR(EBaseUserMessages.UM_ShakeDir_VALUE, CUserMsg_ShakeDir.class),
    STATS_CRAWL_MSG(EBaseUserMessages.UM_StatsCrawlMsg_VALUE, CUserMsg_StatsCrawlMsg.class),
    STATS_SKIP_STATE(EBaseUserMessages.UM_StatsSkipState_VALUE, CUserMsg_StatsSkipState.class),
    TEXT_MSG(EBaseUserMessages.UM_TextMsg_VALUE, CUserMsg_TextMsg.class),
    TILT(EBaseUserMessages.UM_Tilt_VALUE, CUserMsg_Tilt.class),
    TRAIN(EBaseUserMessages.UM_Train_VALUE, CUserMsg_Train.class),
    VGUI_MENU(EBaseUserMessages.UM_VGUIMenu_VALUE, CUserMsg_VGUIMenu.class),
    VOICE_MASK(EBaseUserMessages.UM_VoiceMask_VALUE, CUserMsg_VoiceMask.class),
    VOICE_SUBTITLE(EBaseUserMessages.UM_VoiceSubtitle_VALUE, CUserMsg_VoiceSubtitle.class),
    SEND_AUDIO(EBaseUserMessages.UM_SendAudio_VALUE, CUserMsg_SendAudio.class),

    ADD_UNIT_TO_SELECTION(EDotaUserMessages.DOTA_UM_AddUnitToSelection_VALUE, CDOTAUserMsg_AIDebugLine.class),
    AI_DEBUG_LINE(EDotaUserMessages.DOTA_UM_AIDebugLine_VALUE, CDOTAUserMsg_AIDebugLine.class),
    CHAT_EVENT(EDotaUserMessages.DOTA_UM_ChatEvent_VALUE, CDOTAUserMsg_ChatEvent.class),
    COMBAT_HERO_POSITIONS(EDotaUserMessages.DOTA_UM_CombatHeroPositions_VALUE, CDOTAUserMsg_CombatHeroPositions.class),
    COMBAT_LOG_DATA(EDotaUserMessages.DOTA_UM_CombatLogData_VALUE, CDOTAUserMsg_CombatLogData.class),
    COMBAT_LOG_SHOW_DEATH(EDotaUserMessages.DOTA_UM_CombatLogShowDeath_VALUE, CDOTAUserMsg_CombatLogShowDeath.class),
    CREATE_LINEAR_PROJECTILE(EDotaUserMessages.DOTA_UM_CreateLinearProjectile_VALUE, CDOTAUserMsg_CreateLinearProjectile.class),
    DESTROY_LINEAR_PROJECTILE(EDotaUserMessages.DOTA_UM_DestroyLinearProjectile_VALUE, CDOTAUserMsg_DestroyLinearProjectile.class),
    DODGE_TRACKING_PROJECTILES(EDotaUserMessages.DOTA_UM_DodgeTrackingProjectiles_VALUE, CDOTAUserMsg_DodgeTrackingProjectiles.class),
    GLOBAL_LIGHT_COLOR(EDotaUserMessages.DOTA_UM_GlobalLightColor_VALUE, CDOTAUserMsg_GlobalLightColor.class),
    GLOBAL_LIGHT_DIRECTION(EDotaUserMessages.DOTA_UM_GlobalLightDirection_VALUE, CDOTAUserMsg_GlobalLightDirection.class),
    INVALID_COMMAND(EDotaUserMessages.DOTA_UM_InvalidCommand_VALUE, CDOTAUserMsg_InvalidCommand.class),
    LOCATION_PING(EDotaUserMessages.DOTA_UM_LocationPing_VALUE, CDOTAUserMsg_LocationPing.class),
    MAP_LINE(EDotaUserMessages.DOTA_UM_MapLine_VALUE, CDOTAUserMsg_MapLine.class),
    MINI_KILL_CAM_INFO(EDotaUserMessages.DOTA_UM_MiniKillCamInfo_VALUE, CDOTAUserMsg_MiniKillCamInfo.class),
    MINIMAP_DEBUG_POINT(EDotaUserMessages.DOTA_UM_MinimapDebugPoint_VALUE, CDOTAUserMsg_MinimapDebugPoint.class),
    MINIMAP_EVENT(EDotaUserMessages.DOTA_UM_MinimapEvent_VALUE, CDOTAUserMsg_MinimapEvent.class),
    NEVERMORE_REQUIEM(EDotaUserMessages.DOTA_UM_NevermoreRequiem_VALUE, CDOTAUserMsg_NevermoreRequiem.class),
    OVERHEAD_EVENT(EDotaUserMessages.DOTA_UM_OverheadEvent_VALUE, CDOTAUserMsg_OverheadEvent.class),
    SET_NEXT_AUTOBUY_ITEM(EDotaUserMessages.DOTA_UM_SetNextAutobuyItem_VALUE, CDOTAUserMsg_SetNextAutobuyItem.class),
    SHARED_COOLDOWN(EDotaUserMessages.DOTA_UM_SharedCooldown_VALUE, CDOTAUserMsg_SharedCooldown.class),
    SPECTATOR_PLAYER_CLICK(EDotaUserMessages.DOTA_UM_SpectatorPlayerClick_VALUE, CDOTAUserMsg_SpectatorPlayerClick.class),
    TUTORIAL_TIP_INFO(EDotaUserMessages.DOTA_UM_TutorialTipInfo_VALUE, CDOTAUserMsg_TutorialTipInfo.class),
    UNIT_EVENT(EDotaUserMessages.DOTA_UM_UnitEvent_VALUE, CDOTAUserMsg_UnitEvent.class),
    PARTICLE_MANAGER(EDotaUserMessages.DOTA_UM_ParticleManager_VALUE, CDOTAUserMsg_ParticleManager.class),
    BOT_CHAT(EDotaUserMessages.DOTA_UM_BotChat_VALUE, CDOTAUserMsg_BotChat.class),
    HUD_ERROR(EDotaUserMessages.DOTA_UM_HudError_VALUE, CDOTAUserMsg_HudError.class),
    ITEM_PURCHASED(EDotaUserMessages.DOTA_UM_ItemPurchased_VALUE, CDOTAUserMsg_ItemPurchased.class),
    PING(EDotaUserMessages.DOTA_UM_Ping_VALUE, CDOTAUserMsg_Ping.class),
    ITEM_FOUND(EDotaUserMessages.DOTA_UM_ItemFound_VALUE, CDOTAUserMsg_ItemFound.class),
    CHARACTER_SPEAK_CONCEPT(EDotaUserMessages.DOTA_UM_CharacterSpeakConcept_VALUE, null), // MISSING
    SWAP_VERIFY(EDotaUserMessages.DOTA_UM_SwapVerify_VALUE, CDOTAUserMsg_SwapVerify.class),
    WORLD_LINE(EDotaUserMessages.DOTA_UM_WorldLine_VALUE, CDOTAUserMsg_WorldLine.class),
    TOURNAMENT_DROP(EDotaUserMessages.DOTA_UM_TournamentDrop_VALUE, CDOTAUserMsg_TournamentDrop.class),
    ITEM_ALERT(EDotaUserMessages.DOTA_UM_ItemAlert_VALUE, CDOTAUserMsg_ItemAlert.class),
    HALLOWEEN_DROPS(EDotaUserMessages.DOTA_UM_HalloweenDrops_VALUE, CDOTAUserMsg_HalloweenDrops.class),
    CHAT_WHEEL(EDotaUserMessages.DOTA_UM_ChatWheel_VALUE, CDOTAUserMsg_ChatWheel.class),
    RECEIVED_XMAS_GIFT(EDotaUserMessages.DOTA_UM_ReceivedXmasGift_VALUE, CDOTAUserMsg_ReceivedXmasGift.class),
    UPDATE_SHARED_CONTENT(EDotaUserMessages.DOTA_UM_UpdateSharedContent_VALUE, CDOTAUserMsg_UpdateSharedContent.class),
    TUTORIAL_REQUEST_EXP(EDotaUserMessages.DOTA_UM_TutorialRequestExp_VALUE, CDOTAUserMsg_TutorialRequestExp.class),
    TUTORIAL_PING_MINIMAP(EDotaUserMessages.DOTA_UM_TutorialPingMinimap_VALUE, CDOTAUserMsg_TutorialPingMinimap.class),
    GAMERULES_STATE_CHANGED(EDotaUserMessages.DOTA_UM_GamerulesStateChanged_VALUE, CDOTA_UM_GamerulesStateChanged.class),
    SHOW_SURVEY(EDotaUserMessages.DOTA_UM_ShowSurvey_VALUE, CDOTAUserMsg_ShowSurvey.class),
    TUTORIAL_FADE(EDotaUserMessages.DOTA_UM_TutorialFade_VALUE, CDOTAUserMsg_TutorialFade.class),
    ADD_QUEST_LOG_ENTRY(EDotaUserMessages.DOTA_UM_AddQuestLogEntry_VALUE, CDOTAUserMsg_AddQuestLogEntry.class),
    SEND_STAT_POPUP(EDotaUserMessages.DOTA_UM_SendStatPopup_VALUE, CDOTAUserMsg_SendStatPopup.class),
    TUTORIAL_FINISH(EDotaUserMessages.DOTA_UM_TutorialFinish_VALUE, CDOTAUserMsg_TutorialFinish.class),
    SEND_ROSHAN_POPUP(EDotaUserMessages.DOTA_UM_SendRoshanPopup_VALUE, CDOTAUserMsg_SendRoshanPopup.class),
    SEND_GENERIC_TOOL_TIP(EDotaUserMessages.DOTA_UM_SendGenericToolTip_VALUE, CDOTAUserMsg_SendGenericToolTip.class),
    SEND_FINAL_GOLD(EDotaUserMessages.DOTA_UM_SendFinalGold_VALUE, CDOTAUserMsg_SendFinalGold.class),
    CUSTOM_MSG(EDotaUserMessages.DOTA_UM_CustomMsg_VALUE, CDOTAUserMsg_CustomMsg.class),
    COACH_HUD_PING(EDotaUserMessages.DOTA_UM_CoachHUDPing_VALUE, CDOTAUserMsg_CoachHUDPing.class),
    CLIENT_LOAD_GRID_NAV(EDotaUserMessages.DOTA_UM_ClientLoadGridNav_VALUE, CDOTAUserMsg_ClientLoadGridNav.class);

    private final int id;
    private final Class<? extends GeneratedMessage> clazz;
    private final Method parseMethod;
    
    private UserMessageType() {
        this(-1, null);
    }
    
    private UserMessageType(int id, Class<? extends GeneratedMessage> clazz) {
        this.id = id;
        this.clazz = clazz;
        if (clazz != null) {
            try {
                this.parseMethod = clazz.getMethod("parseFrom", ByteString.class);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new RuntimeException(e);
            } 
        } else {
            this.parseMethod = null;
        }
    }
    
    public int getId() {
        return id;
    }
    
    public Class<? extends GeneratedMessage> getClazz() {
        return clazz;
    }

    public GeneratedMessage parseFrom(ByteString data) {
        try {
            return (GeneratedMessage) parseMethod.invoke(null, data);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static UserMessageType forId(int id) {
        for (UserMessageType t : values()) {
            if (t.id == id) {
                return t;
            }
        }
        return null;
    }
    

}