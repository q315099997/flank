package ftl.args

import com.linkedin.dex.parser.DexParser
import com.linkedin.dex.parser.TestMethod
import ftl.config.FtlConstants
import ftl.filter.TestFilter
import ftl.filter.TestFilters
import ftl.gc.GcStorage
import ftl.util.FlankTestMethod
import java.io.File

object AndroidTestShard {

    // computed properties not specified in yaml
    fun getTestShardChunks(args: AndroidArgs, testApk: String): ShardChunks {
        // Download test APK if necessary so it can be used to validate test methods
        val testLocalApk = if (testApk.startsWith(FtlConstants.GCS_PREFIX))
            GcStorage.download(testApk) else
            testApk

        val filteredTests = getTestMethods(args, testLocalApk)

        if (filteredTests.isEmpty()) println("${FtlConstants.indent}No tests for ${testLocalApk.apkFileName}")

        return if (args.numUniformShards == null)
            ArgsHelper.calculateShards(filteredTests, args) else
            listOf(filteredTests.map(FlankTestMethod::testName))
    }

    private fun getTestMethods(args: AndroidArgs, testLocalApk: String): List<FlankTestMethod> {
        val allTestMethods = DexParser.findTestMethods(testLocalApk)
        if (allTestMethods.isEmpty()) {
            // Avoid unnecessary computation if we already know there aren't tests.
            return emptyList()
        }
        val testFilter = TestFilters.fromTestTargets(args.testTargets)
        return allTestMethods filterWith testFilter
    }

    private infix fun List<TestMethod>.filterWith(filter: TestFilter) = asSequence()
        .distinct()
        .filter(filter.shouldRun)
        .map { FlankTestMethod("class ${it.testName}", it.isIgnored) }
        .toList()
}

private val TestMethod.isIgnored: Boolean
    get() = annotations.map { it.name }.contains("org.junit.Ignore")

private inline val String.apkFileName: String
    get() = File(this).name
