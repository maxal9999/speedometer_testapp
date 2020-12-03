#include <cmath>
#include <ctime>
#include <future>
#include <random>
#include <stdexcept>

#include <NeedlePositionGenerator.h>

namespace generate_service
{

namespace
{

int get_random_number(int max_generated_value)
{
	if (max_generated_value <= 0)
	{
		return max_generated_value;
	}

	std::mt19937 gen(time(0));
	std::uniform_int_distribution<> uid(max_generated_value / 2, max_generated_value);
	return uid(gen);
}

} // namespace

using namespace std::chrono_literals;
using namespace std::string_literals;

NeedlePositionGenerator & NeedlePositionGenerator::Instance()
{
	static NeedlePositionGenerator generator;
	return generator;
}

void NeedlePositionGenerator::set_enviroment(JNIEnv * jni_env, jobject generate_service, double max_generated_value)
{
	if (!jni_env)
	{
		throw std::invalid_argument{"Empty jni_env"};
	}

	const std::lock_guard<std::mutex> lock(mutex_);
	jni_env->GetJavaVM(&jvm_);
	object_ref_ = jni_env->NewGlobalRef(generate_service);
	jclass j_class = jni_env->GetObjectClass(generate_service);
	on_value_changed_ = jni_env->GetMethodID(j_class, "onValueChanged", "(I)V");
	max_generated_value_ = max_generated_value;
	init_value_ = get_random_number(max_generated_value_);
}

void NeedlePositionGenerator::start_generate()
{
	interrupt_generate_flag_ = false;
	std::thread(
		[this]()
		{
			std::unique_lock<std::mutex> locker(mutex_);
			JNIEnv * jni_env = get_jni_env();
			while (true)
			{
				if (!condition_variable_.wait_for(
						locker,
						1s,
						[&]
						{
							return interrupt_generate_flag_.load();
						}))
				{
					const auto now_time = std::chrono::steady_clock::now();
					const auto int_time = now_time.time_since_epoch().count();
					int motion_value = std::abs(init_value_ * (5 * sin(int_time) + 0.2 * sin(2 * int_time)));
					while (motion_value > max_generated_value_)
					{
						motion_value -= max_generated_value_;
					}
					jni_env->CallVoidMethod(object_ref_, on_value_changed_, jint(motion_value));
				}
				else
				{
					break;
				}
			}
		})
		.detach();
}

void NeedlePositionGenerator::stop_generate()
{
	interrupt_generate_flag_ = true;
	condition_variable_.notify_one();
}

JNIEnv * NeedlePositionGenerator::get_jni_env()
{
	JavaVMAttachArgs attach_args;
	attach_args.version = JNI_VERSION_1_6;
	attach_args.name = ">>>NativeThread__Any";
	attach_args.group = nullptr;
	JNIEnv * env;
	if (jvm_->AttachCurrentThread(&env, &attach_args) != JNI_OK)
	{
		env = nullptr;
	}

	return env;
}

} // namespace generate_service
