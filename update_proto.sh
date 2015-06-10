#!/bin/sh
pushd proto
wget -N https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/ai_activity.proto
wget -N https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/demo.proto
wget -N https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/dota_commonmessages.proto
wget -N https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/dota_modifiers.proto
wget -N https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/dota_usermessages.proto
wget -N https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/netmessages.proto
wget -N https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/networkbasetypes.proto
wget -N https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/usermessages.proto
wget -N https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/network_connection.proto
protoc --java_out=../src/main/java/skadistats/clarity/wire/proto *.proto
popd