package skadistats.clarity.wire;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.wire.proto.*;

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;

public class PacketTypes {

    public static final Map<Integer, Class<? extends GeneratedMessage>> DEMO;
    static {
        DEMO = new HashMap<Integer, Class<? extends GeneratedMessage>>();
        DEMO.put(Demo.EDemoCommands.DEM_ClassInfo_VALUE, Demo.CDemoClassInfo.class);
        DEMO.put(Demo.EDemoCommands.DEM_ConsoleCmd_VALUE, Demo.CDemoConsoleCmd.class);
        DEMO.put(Demo.EDemoCommands.DEM_CustomData_VALUE, Demo.CDemoCustomData.class);
        DEMO.put(Demo.EDemoCommands.DEM_CustomDataCallbacks_VALUE, Demo.CDemoCustomDataCallbacks.class);
        DEMO.put(Demo.EDemoCommands.DEM_FileHeader_VALUE, Demo.CDemoFileHeader.class);
        DEMO.put(Demo.EDemoCommands.DEM_FileInfo_VALUE, Demo.CDemoFileInfo.class);
        DEMO.put(Demo.EDemoCommands.DEM_FullPacket_VALUE, Demo.CDemoFullPacket.class);
        DEMO.put(Demo.EDemoCommands.DEM_Packet_VALUE, Demo.CDemoPacket.class);
        DEMO.put(Demo.EDemoCommands.DEM_SendTables_VALUE, Demo.CDemoSendTables.class);
        DEMO.put(Demo.EDemoCommands.DEM_SignonPacket_VALUE, Demo.CDemoPacket.class);
        DEMO.put(Demo.EDemoCommands.DEM_StringTables_VALUE, Demo.CDemoStringTables.class);
        DEMO.put(Demo.EDemoCommands.DEM_Stop_VALUE, Demo.CDemoStop.class);
        DEMO.put(Demo.EDemoCommands.DEM_SyncTick_VALUE, Demo.CDemoSyncTick.class);
        DEMO.put(Demo.EDemoCommands.DEM_UserCmd_VALUE, Demo.CDemoUserCmd.class);
        DEMO.put(Demo.EDemoCommands.DEM_SaveGame_VALUE, Demo.CDemoSaveGame.class);
    }
    
    public static final Map<Integer, Class<? extends GeneratedMessage>> EMBED;
    static {
        EMBED = new HashMap<Integer, Class<? extends GeneratedMessage>>();
        EMBED.put(Networkbasetypes.NET_Messages.net_SetConVar_VALUE, Networkbasetypes.CNETMsg_SetConVar.class);
        EMBED.put(Networkbasetypes.NET_Messages.net_SignonState_VALUE, Networkbasetypes.CNETMsg_SignonState.class);
        EMBED.put(Networkbasetypes.NET_Messages.net_Tick_VALUE, Networkbasetypes.CNETMsg_Tick.class);
        EMBED.put(Netmessages.SVC_Messages.svc_ClassInfo_VALUE, Netmessages.CSVCMsg_ClassInfo.class);
        EMBED.put(Netmessages.SVC_Messages.svc_CreateStringTable_VALUE, Netmessages.CSVCMsg_CreateStringTable.class);
        EMBED.put(Netmessages.SVC_Messages.svc_GameEvent_VALUE, Networkbasetypes.CSVCMsg_GameEvent.class);
        EMBED.put(Netmessages.SVC_Messages.svc_GameEventList_VALUE, Netmessages.CSVCMsg_GameEventList.class);
        EMBED.put(Netmessages.SVC_Messages.svc_Menu_VALUE, Netmessages.CSVCMsg_Menu.class);
        EMBED.put(Netmessages.SVC_Messages.svc_PacketEntities_VALUE, Netmessages.CSVCMsg_PacketEntities.class);
        EMBED.put(Netmessages.SVC_Messages.svc_SendTable_VALUE, Netmessages.CSVCMsg_SendTable.class);
        EMBED.put(Netmessages.SVC_Messages.svc_ServerInfo_VALUE, Netmessages.CSVCMsg_ServerInfo.class);
        EMBED.put(Netmessages.SVC_Messages.svc_SetView_VALUE, Netmessages.CSVCMsg_SetView.class);
        EMBED.put(Netmessages.SVC_Messages.svc_Sounds_VALUE, Netmessages.CSVCMsg_Sounds.class);
        EMBED.put(Netmessages.SVC_Messages.svc_TempEntities_VALUE, Netmessages.CSVCMsg_TempEntities.class);
        EMBED.put(Netmessages.SVC_Messages.svc_UpdateStringTable_VALUE, Netmessages.CSVCMsg_UpdateStringTable.class);
        EMBED.put(Netmessages.SVC_Messages.svc_UserMessage_VALUE, Networkbasetypes.CSVCMsg_UserMessage.class);
        EMBED.put(Netmessages.SVC_Messages.svc_VoiceInit_VALUE, Netmessages.CSVCMsg_VoiceInit.class);
        EMBED.put(Netmessages.SVC_Messages.svc_VoiceData_VALUE, Netmessages.CSVCMsg_VoiceData.class);

        EMBED.put(Netmessages.SVC_Messages.svc_Print_VALUE, Netmessages.CSVCMsg_Print.class);
        EMBED.put(Netmessages.SVC_Messages.svc_FullFrameSplit_VALUE, Netmessages.CSVCMsg_FullFrameSplit.class);
    }
    
    public static final Map<Integer, Class<? extends GeneratedMessage>> USERMSG;
    static {
        USERMSG = new HashMap<Integer, Class<? extends GeneratedMessage>>();
        USERMSG.put(Usermessages.EBaseUserMessages.UM_AchievementEvent_VALUE, Usermessages.CUserMsg_AchievementEvent.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_CloseCaption_VALUE, Usermessages.CUserMsg_CloseCaption.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_CloseCaptionDirect_VALUE, Usermessages.CUserMsg_CloseCaption.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_CurrentTimescale_VALUE, Usermessages.CUserMsg_CurrentTimescale.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_DesiredTimescale_VALUE, Usermessages.CUserMsg_DesiredTimescale.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_Fade_VALUE, Usermessages.CUserMsg_Fade.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_GameTitle_VALUE, Usermessages.CUserMsg_GameTitle.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_Geiger_VALUE, Usermessages.CUserMsg_Geiger.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_HintText_VALUE, Usermessages.CUserMsg_HintText.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_HudMsg_VALUE, Usermessages.CUserMsg_HudMsg.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_HudText_VALUE, Usermessages.CUserMsg_HudText.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_KeyHintText_VALUE, Usermessages.CUserMsg_KeyHintText.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_MessageText_VALUE, Usermessages.CUserMsg_MessageText.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_RequestState_VALUE, Usermessages.CUserMsg_RequestState.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_ResetHUD_VALUE, Usermessages.CUserMsg_ResetHUD.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_Rumble_VALUE, Usermessages.CUserMsg_Rumble.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_SayText_VALUE, Usermessages.CUserMsg_SayText.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_SayText2_VALUE, Usermessages.CUserMsg_SayText2.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_SayTextChannel_VALUE, Usermessages.CUserMsg_SayTextChannel.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_Shake_VALUE, Usermessages.CUserMsg_Shake.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_ShakeDir_VALUE, Usermessages.CUserMsg_ShakeDir.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_StatsCrawlMsg_VALUE, Usermessages.CUserMsg_StatsCrawlMsg.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_StatsSkipState_VALUE, Usermessages.CUserMsg_StatsSkipState.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_TextMsg_VALUE, Usermessages.CUserMsg_TextMsg.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_Tilt_VALUE, Usermessages.CUserMsg_Tilt.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_Train_VALUE, Usermessages.CUserMsg_Train.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_VGUIMenu_VALUE, Usermessages.CUserMsg_VGUIMenu.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_VoiceMask_VALUE, Usermessages.CUserMsg_VoiceMask.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_VoiceSubtitle_VALUE, Usermessages.CUserMsg_VoiceSubtitle.class);
        USERMSG.put(Usermessages.EBaseUserMessages.UM_SendAudio_VALUE, Usermessages.CUserMsg_SendAudio.class);
        
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_AddUnitToSelection_VALUE, DotaUsermessages.CDOTAUserMsg_AIDebugLine.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_AIDebugLine_VALUE, DotaUsermessages.CDOTAUserMsg_AIDebugLine.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_ChatEvent_VALUE, DotaUsermessages.CDOTAUserMsg_ChatEvent.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_CombatHeroPositions_VALUE, DotaUsermessages.CDOTAUserMsg_CombatHeroPositions.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_CombatLogData_VALUE, DotaUsermessages.CDOTAUserMsg_CombatLogData.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_CombatLogShowDeath_VALUE, DotaUsermessages.CDOTAUserMsg_CombatLogShowDeath.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_CreateLinearProjectile_VALUE, DotaUsermessages.CDOTAUserMsg_CreateLinearProjectile.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_DestroyLinearProjectile_VALUE, DotaUsermessages.CDOTAUserMsg_DestroyLinearProjectile.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_DodgeTrackingProjectiles_VALUE, DotaUsermessages.CDOTAUserMsg_DodgeTrackingProjectiles.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_GlobalLightColor_VALUE, DotaUsermessages.CDOTAUserMsg_GlobalLightColor.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_GlobalLightDirection_VALUE, DotaUsermessages.CDOTAUserMsg_GlobalLightDirection.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_InvalidCommand_VALUE, DotaUsermessages.CDOTAUserMsg_InvalidCommand.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_LocationPing_VALUE, DotaUsermessages.CDOTAUserMsg_LocationPing.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_MapLine_VALUE, DotaUsermessages.CDOTAUserMsg_MapLine.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_MiniKillCamInfo_VALUE, DotaUsermessages.CDOTAUserMsg_MiniKillCamInfo.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_MinimapDebugPoint_VALUE, DotaUsermessages.CDOTAUserMsg_MinimapDebugPoint.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_MinimapEvent_VALUE, DotaUsermessages.CDOTAUserMsg_MinimapEvent.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_NevermoreRequiem_VALUE, DotaUsermessages.CDOTAUserMsg_NevermoreRequiem.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_OverheadEvent_VALUE, DotaUsermessages.CDOTAUserMsg_OverheadEvent.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_SetNextAutobuyItem_VALUE, DotaUsermessages.CDOTAUserMsg_SetNextAutobuyItem.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_SharedCooldown_VALUE, DotaUsermessages.CDOTAUserMsg_SharedCooldown.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_SpectatorPlayerClick_VALUE, DotaUsermessages.CDOTAUserMsg_SpectatorPlayerClick.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_TutorialTipInfo_VALUE, DotaUsermessages.CDOTAUserMsg_TutorialTipInfo.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_UnitEvent_VALUE, DotaUsermessages.CDOTAUserMsg_UnitEvent.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_ParticleManager_VALUE, DotaUsermessages.CDOTAUserMsg_ParticleManager.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_BotChat_VALUE, DotaUsermessages.CDOTAUserMsg_BotChat.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_HudError_VALUE, DotaUsermessages.CDOTAUserMsg_HudError.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_ItemPurchased_VALUE, DotaUsermessages.CDOTAUserMsg_ItemPurchased.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_Ping_VALUE, DotaUsermessages.CDOTAUserMsg_Ping.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_ItemFound_VALUE, DotaUsermessages.CDOTAUserMsg_ItemFound.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_CharacterSpeakConcept_VALUE, null); // MISSING
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_SwapVerify_VALUE, DotaUsermessages.CDOTAUserMsg_SwapVerify.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_WorldLine_VALUE, DotaUsermessages.CDOTAUserMsg_WorldLine.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_TournamentDrop_VALUE, DotaUsermessages.CDOTAUserMsg_TournamentDrop.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_ItemAlert_VALUE, DotaUsermessages.CDOTAUserMsg_ItemAlert.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_HalloweenDrops_VALUE, DotaUsermessages.CDOTAUserMsg_HalloweenDrops.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_ChatWheel_VALUE, DotaUsermessages.CDOTAUserMsg_ChatWheel.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_ReceivedXmasGift_VALUE, DotaUsermessages.CDOTAUserMsg_ReceivedXmasGift.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_UpdateSharedContent_VALUE, DotaUsermessages.CDOTAUserMsg_UpdateSharedContent.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_TutorialRequestExp_VALUE, DotaUsermessages.CDOTAUserMsg_TutorialRequestExp.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_TutorialPingMinimap_VALUE, DotaUsermessages.CDOTAUserMsg_TutorialPingMinimap.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_GamerulesStateChanged_VALUE, DotaUsermessages.CDOTA_UM_GamerulesStateChanged.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_ShowSurvey_VALUE, DotaUsermessages.CDOTAUserMsg_ShowSurvey.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_TutorialFade_VALUE, DotaUsermessages.CDOTAUserMsg_TutorialFade.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_AddQuestLogEntry_VALUE, DotaUsermessages.CDOTAUserMsg_AddQuestLogEntry.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_SendStatPopup_VALUE, DotaUsermessages.CDOTAUserMsg_SendStatPopup.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_TutorialFinish_VALUE, DotaUsermessages.CDOTAUserMsg_TutorialFinish.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_SendRoshanPopup_VALUE, DotaUsermessages.CDOTAUserMsg_SendRoshanPopup.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_SendGenericToolTip_VALUE, DotaUsermessages.CDOTAUserMsg_SendGenericToolTip.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_SendFinalGold_VALUE, DotaUsermessages.CDOTAUserMsg_SendFinalGold.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_CustomMsg_VALUE, DotaUsermessages.CDOTAUserMsg_CustomMsg.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_CoachHUDPing_VALUE, DotaUsermessages.CDOTAUserMsg_CoachHUDPing.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_ClientLoadGridNav_VALUE, DotaUsermessages.CDOTAUserMsg_ClientLoadGridNav.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_AbilityPing_VALUE, DotaUsermessages.CDOTAUserMsg_AbilityPing.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_ShowGenericPopup_VALUE, DotaUsermessages.CDOTAUserMsg_ShowGenericPopup.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_VoteStart_VALUE, DotaUsermessages.CDOTAUserMsg_VoteStart.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_VoteUpdate_VALUE, DotaUsermessages.CDOTAUserMsg_VoteUpdate.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_VoteEnd_VALUE, DotaUsermessages.CDOTAUserMsg_VoteEnd.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_BoosterState_VALUE, DotaUsermessages.CDOTAUserMsg_BoosterState.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_WillPurchaseAlert_VALUE, DotaUsermessages.CDOTAUserMsg_WillPurchaseAlert.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_TutorialMinimapPosition_VALUE, DotaUsermessages.CDOTAUserMsg_TutorialMinimapPosition.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_PlayerMMR_VALUE, DotaUsermessages.CDOTAUserMsg_PlayerMMR.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_AbilitySteal_VALUE, DotaUsermessages.CDOTAUserMsg_AbilitySteal.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_CourierKilledAlert_VALUE, DotaUsermessages.CDOTAUserMsg_CourierKilledAlert.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_EnemyItemAlert_VALUE, DotaUsermessages.CDOTAUserMsg_EnemyItemAlert.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_StatsMatchDetails_VALUE, DotaUsermessages.CDOTAUserMsg_StatsMatchDetails.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_MiniTaunt_VALUE, DotaUsermessages.CDOTAUserMsg_MiniTaunt.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_BuyBackStateAlert_VALUE, DotaUsermessages.CDOTAUserMsg_BuyBackStateAlert.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_QuickBuyAlert_VALUE, DotaUsermessages.CDOTAUserMsg_QuickBuyAlert.class);
        USERMSG.put(DotaUsermessages.EDotaUserMessages.DOTA_UM_StatsHeroDetails_VALUE, DotaUsermessages.CDOTAUserMsg_StatsHeroMinuteDetails.class);
    }

    private static final Map<Class<? extends GeneratedMessage>, MethodHandle> PARSE_METHODS = new HashMap<Class<? extends GeneratedMessage>, MethodHandle>() {
        private static final long serialVersionUID = -6842762498712492043L;
        @SuppressWarnings("unchecked")
        @Override
        public MethodHandle get(Object key) {
            MethodHandle m = super.get(key);
            if (m == null) {
                try {
                    m = new ConstantCallSite(
                        MethodHandles.publicLookup().findStatic(
                            (Class<? extends GeneratedMessage>) key,
                            "parseFrom",
                            MethodType.methodType((Class<? extends GeneratedMessage>) key, ByteString.class)
                        )
                    ).dynamicInvoker();
                    put((Class<? extends GeneratedMessage>) key, m);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return m;
        }
    };

    @SuppressWarnings("unchecked")
    public static <T extends GeneratedMessage> T parse(Class<T> clazz, ByteString data) {
        try {
            return (T) PARSE_METHODS.get(clazz).invoke(data);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
    
}
