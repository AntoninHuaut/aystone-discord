package model

type AystoneInstance struct {
	Name                    string `db:"name"`
	Visible                 bool   `db:"visible"`
	MaxPlayer               int    `db:"max_player"`
	IPAddress               string `db:"ip_address"`
	Port                    int    `db:"port"`
	Enabled                 bool   `db:"enabled"`
	CnfApplied              bool   `db:"cnfapplied"`
	NumberRegisteredPlayers int    `db:"number_registered_players"`
}
