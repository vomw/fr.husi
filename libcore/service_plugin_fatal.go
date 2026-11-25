package libcore

// PluginFatalHandler is reverse-bound from Kotlin so a crashing plugin can stop
// the Android service.
type PluginFatalHandler interface {
	OnPluginFatal(message string)
}
