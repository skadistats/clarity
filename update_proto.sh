#!/bin/sh
pushd proto
rm *.proto
wget https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/ai_activity.proto
wget https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/demo.proto
wget https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/dota_commonmessages.proto
wget https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/dota_modifiers.proto
wget https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/dota_usermessages.proto
wget https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/netmessages.proto
wget https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/networkbasetypes.proto
wget https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/usermessages.proto
wget https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/network_connection.proto

wget https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/dota_gcmessages_common.proto
wget https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/steammessages.proto
wget https://raw.githubusercontent.com/SteamDatabase/GameTracking/master/Protobufs/dota/gcsdk_gcmessages.proto


sed -i '1i option java_package = "skadistats.clarity.wire.proto";' *.proto
protoc --java_out=../src/main/java *.proto
popd