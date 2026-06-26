// Test script for LanguageUtils
import io.github.solas.mcp.weather.server.LanguageUtils

println "Testing LanguageUtils..."

def moscowRu = LanguageUtils.isRussian("Москва")
def moscowEn = LanguageUtils.isRussian("Moscow")
def munchen = LanguageUtils.isRussian("München")
def localeRu = LanguageUtils.getLocale("Москва")
def localeEn = LanguageUtils.getLocale("Moscow")

println "isRussian(\"Москва\"): ${moscowRu} (expected: true)"
println "isRussian(\"Moscow\"): ${moscowEn} (expected: false)"
println "isRussian(\"München\"): ${munchen} (expected: false)"
println "getLocale(\"Москва\"): ${localeRu} (expected: ru)"
println "getLocale(\"Moscow\"): ${localeEn} (expected: en)"

def allPass = moscowRu == true && moscowEn == false && munchen == false && localeRu == "ru" && localeEn == "en"
println "\nAll tests passed: ${allPass}"

if (!allPass) {
    System.exit(1)
}
