package org.catrobat.catroid.content.actions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.catrobat.catroid.formulaeditor.UserData
import org.catrobat.catroid.io.DeviceUserDataAccessor

class WriteUserDataOnDeviceAction : AsynchronousAction() {
    var userData: UserData<Any>? = null
    var accessor: DeviceUserDataAccessor? = null

    @Volatile
    private var writeActionFinished = false
    private var job: Job = SupervisorJob()

    override fun initialize() {
        writeActionFinished = false
        job = SupervisorJob()
        userData?.let { executeWriteTask(it) }
    }

    override fun restart() {
        job.cancel()
        writeActionFinished = false
        super.restart()
    }

    override fun isFinished(): Boolean = writeActionFinished || userData == null

    private fun executeWriteTask(userData: UserData<Any>) {
        CoroutineScope(Dispatchers.IO + job).launch {
            accessor?.writeUserData(userData)
            withContext(Dispatchers.Main) {
                writeActionFinished = true
            }
        }
    }
}