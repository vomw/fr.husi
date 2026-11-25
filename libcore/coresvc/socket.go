package coresvc

import "path/filepath"

const Socket = "api.sock"

func SocketPath(basePath string) string {
	return filepath.Join(basePath, Socket)
}
