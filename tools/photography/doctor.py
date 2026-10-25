"""Read-only hardware probe. No model download, API calls or paid service setup."""
import importlib.util
import json
import platform
import shutil
import subprocess


def command(args):
    try:
        return subprocess.check_output(args, text=True, stderr=subprocess.DEVNULL, timeout=15).strip()
    except (OSError, subprocess.SubprocessError):
        return None


def main():
    result = dict(os=platform.system(), architecture=platform.machine(),
                  torchInstalled=importlib.util.find_spec('torch') is not None,
                  diffusersInstalled=importlib.util.find_spec('diffusers') is not None,
                  note='Describes only the machine running this command; does not access another Mac.')
    if platform.system() == 'Darwin':
        result['model'] = command(['sysctl', '-n', 'hw.model'])
        result['chip'] = command(['sysctl', '-n', 'machdep.cpu.brand_string'])
        memory = command(['sysctl', '-n', 'hw.memsize'])
        result['memoryGiB'] = round(int(memory) / 2**30, 1) if memory else None
        result['next'] = 'Evaluate a local model and its license, then compare a pilot batch with approved photographs. No generation installed or started.'
    else:
        result['nvidiaGpu'] = command(['nvidia-smi', '--query-gpu=name,memory.total', '--format=csv,noheader']) if shutil.which('nvidia-smi') else None
        result['next'] = 'The included chat image tool is not a Python API. Run this probe on the actual Mac to assess local generation.'
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
